package org.omgcobra.plugins

import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.features.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.html.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.websocket.*
import kotlinx.html.HTML
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.transactions.transaction
import org.omgcobra.*
import org.omgcobra.db.Room
import java.util.*

@KtorExperimentalLocationsAPI
private val rooms = mutableMapOf<String, OldRoom>()

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
      } catch (e: Exception) {
      } finally {
        connectionMap.remove(this)
        connectionMap.updateParticipants()
      }
    }

    authenticate("auth-digest") {
      get<OldRoom.Create> {
        val principal = call.principal<UserIdPrincipal>() ?: throw AuthenticationException()
        call.respondText(when (rooms[it.room.name]) {
          null -> {
            it.room.creator = principal.name
            rooms[it.room.name] = it.room
            "Now ${rooms.size} rooms are open"
          }
          else -> "That room already exists"
        })
      }

      get<OldRoom.Delete> {
        val principal = call.principal<UserIdPrincipal>() ?: throw AuthenticationException()
        call.respondText(when (rooms[it.room.name]?.creator) {
          principal.name -> {
            rooms.remove(it.room.name)
            "Now ${rooms.size} rooms are open"
          }
          null           -> "That room doesn't exist"
          else           -> "That isn't your room"
        })
      }
    }

    static("static") {
      resources()
    }

    get("/rooms") {
      val rooms = transaction { Room.all().map { "${it.name} : ${it.user.name}" } }
      call.respond(rooms)
    }

    get<OldRoom> {
      call.respondText(
          when (val room = rooms[it.name]) {
            null -> "Room not found"
            else -> "Room named ${room.name} is open, owned by ${room.creator}"
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
class OldRoom(val name: String, var creator: String = "system") {
  @Location("/create")
  class Create(val room: OldRoom)

  @Location("/delete")
  class Delete(val room: OldRoom)
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
