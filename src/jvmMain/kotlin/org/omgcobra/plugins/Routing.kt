package org.omgcobra.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.websocket.*
import kotlinx.html.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.transactions.transaction
import org.omgcobra.*
import org.omgcobra.db.*
import java.util.*
import kotlin.collections.Collection
import kotlin.collections.Map
import kotlin.collections.distinct
import kotlin.collections.forEach
import kotlin.collections.hashMapOf
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.collections.single
import kotlin.collections.singleOrNull
import kotlin.collections.toSet

private val previousMessages = mutableListOf<ChatMessage>()
private val connectionMap = Collections.synchronizedMap(mutableMapOf<WebSocketSession, String>())

@ExperimentalSerializationApi
private suspend inline fun <reified T> Collection<WebSocketSession>.sendJson(value: T) =
  forEach { it.send(Json.encodeToString(value = value)) }

@ExperimentalSerializationApi
private suspend inline fun <reified T> WebSocketSession.sendJson(value: T) = listOf(this).sendJson(value)

@ExperimentalSerializationApi
private suspend fun Map<WebSocketSession, String>.updateParticipants() =
  keys.sendJson(UpdateParticipants(values.distinct().toSet()))

val JWTPrincipal.username: String get() = payload.getClaim("username").asString()

@ExperimentalSerializationApi
@KtorExperimentalLocationsAPI
fun Application.configureRouting() {
  install(Locations)

  routing {
    get("/") {
      call.respondHtml(block = HTML::index)
    }

    webSocket("/chat/{author}") {
      val author = call.parameters["author"] ?: "Anonymous"
      connectionMap[this] = author

      sendJson(JoinChat(previousMessages, author))
      connectionMap.updateParticipants()

      try {
        for (frame in incoming) {
          frame as? Frame.Text ?: continue

          val chatMessage = Json.decodeFromString<ChatMessage>(frame.readText())

          connectionMap.keys.sendJson(chatMessage)
          previousMessages.add(chatMessage)
        }
      } catch (_: Exception) {
      } finally {
        connectionMap.remove(this)
        connectionMap.updateParticipants()
      }
    }

    post("/login") {
      val user = call.receive<UserCredential>()

      if (user.username == "spruitt1" && user.password != "tyler") {
        call.respondText("Incorrect password", status = HttpStatusCode.Unauthorized)
      } else {
        val token = JWT.create()
          .withAudience(audience)
          .withIssuer(issuer)
          .withClaim("username", user.username)
          .withExpiresAt(Date(System.currentTimeMillis() + 60_000))
          .sign(Algorithm.HMAC256(secret))

        call.respond(hashMapOf("token" to token))
      }
    }

    authenticate("auth-jwt") {
      get<RoomRequest.Create> {
        val principal = call.principal<JWTPrincipal>() ?: throw AuthenticationException()
        val username = principal.username
        val admin = username == "spruitt1"
        var code = HttpStatusCode.OK
        val response = transaction {
          when {
            !admin                                               -> {
              code = HttpStatusCode.BadRequest
              "You can't create rooms"
            }
            Room.find { Rooms.name.eq(it.request.name) }.empty() -> {
              val room = Room.new {
                name = it.request.name
                user = User.find { Users.name.eq(username) }.single()
              }
              "Created room ${room.id}"
            }

            else                                                 -> {
              code = HttpStatusCode.BadRequest
              "That room already exists"
            }
          }
        }
        call.respondText(response, status = code)
      }

      get<RoomRequest.Delete> {
        val principal = call.principal<JWTPrincipal>() ?: throw AuthenticationException()
        var code = HttpStatusCode.OK
        val response = transaction {
          when (val toDelete = Room.find(Rooms.name.eq(it.request.name)).singleOrNull()) {
            null -> {
              code = HttpStatusCode.BadRequest
              "Room does not exist"
            }
            else -> {
              val user = User.find(Users.name.eq(principal.username)).singleOrNull()
              when {
                user != null && toDelete.user.id == user.id -> {
                  toDelete.delete()
                  "Deleted room"
                }
                else -> {
                  code = HttpStatusCode.BadRequest
                  "That isn't your room"
                }
              }
            }
          }
        }
        call.respondText(response, status = code)
      }

      get("/rooms") {
        val principal = call.principal<JWTPrincipal>() ?: throw AuthenticationException()
        val username = principal.username
        val rooms = transaction {
          val rooms = when (val user = User.find(Users.name.eq(username)).singleOrNull()) {
            null -> Room.all()
            else -> Room.find(Rooms.user.neq(user.id))
          }
          rooms.map(Room::dto)
        }
        call.respond(rooms)
      }

      get("/myRooms") {
        val principal = call.principal<JWTPrincipal>() ?: throw AuthenticationException()
        val username = principal.username
        call.respond(transaction {
          val user = User.find(Users.name.eq(username)).singleOrNull()
          if (user == null) {
            listOf()
          } else {
            Room.find(Rooms.user.eq(user.id)).map(Room::dto)
          }
        })
      }
    }

    static("static") {
      resources()
    }

    get<RoomRequest> {
      call.respondText(transaction {
        when (val room = Room.find { Rooms.name.eq(it.name) }.singleOrNull()) {
          null -> "Room not found"
          else -> "Room named ${room.name} is open, owned by ${room.user.name}"
        }
      })
    }

    install(StatusPages) {
      exception<AuthenticationException> {
        call.respond(HttpStatusCode.Unauthorized)
      }
      exception<AuthorizationException> {
        call.respond(HttpStatusCode.Forbidden)
      }
    }
  }
}

@KtorExperimentalLocationsAPI
@Location("/room/{name}")
class RoomRequest(val name: String) {
  @Location("/create")
  class Create(val request: RoomRequest)

  @Location("/delete")
  class Delete(val request: RoomRequest)
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
