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
import org.omgcobra.*
import java.util.*

@KtorExperimentalLocationsAPI
private val rooms = mutableMapOf<String, Room>()

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
      get<Room.Create> {
        val principal = call.principal<UserIdPrincipal>() ?: throw AuthenticationException()
        when (rooms[it.room.name]) {
          null -> {
            it.room.creator = principal.name
            rooms[it.room.name] = it.room
            call.respondText("Now ${rooms.size} rooms are open")
          }
          else -> call.respondText("That room already exists")
        }
      }

      get<Room.Delete> {
        val principal = call.principal<UserIdPrincipal>() ?: throw AuthenticationException()
        when (rooms[it.room.name]?.creator) {
          principal.name -> {
            rooms.remove(it.room.name)
            call.respondText("Now ${rooms.size} rooms are open")
          }
          null           -> call.respondText("That room doesn't exist")
          else           -> call.respondText("That isn't your room")
        }
      }
    }

    static("static") {
      resources()
    }

    get("/rooms") {
      call.respond(rooms.keys)
    }

    get<Room> {
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
class Room(val name: String, var creator: String = "system") {
  @Location("/create")
  class Create(val room: Room)

  @Location("/delete")
  class Delete(val room: Room)
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
