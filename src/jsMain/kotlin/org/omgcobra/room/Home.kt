package org.omgcobra.room

import kotlinx.browser.window
import kotlinx.coroutines.*
import org.omgcobra.RoomDTO
import org.omgcobra.api.doGet
import org.omgcobra.hooks.useLaunch
import react.*
import react.dom.*
import react.router.dom.RouteComponentProps

val Home: FunctionComponent<RouteComponentProps> = functionComponent(::Home.name) {
  var rooms by useState(setOf<RoomDTO>())
  var myRooms by useState(setOf<RoomDTO>())

  val (_, token) = useContext(AuthenticationContext)

  useLaunch(token) {
    rooms = token?.let { doGet("rooms", token = it) } ?: setOf()
    myRooms = token?.let { doGet("myRooms", token = it) } ?: setOf()
  }

  p {
    +"Home"
  }
  token?.let { jwt ->
    fun attempt(stuffToDo: suspend CoroutineScope.() -> Unit) {
      MainScope().launch {
        try {
          stuffToDo()
          rooms = doGet("rooms", token = jwt)
          myRooms = doGet("myRooms", token = jwt)
        } catch (e: Exception) {
          window.alert(e.message.toString())
        }
      }
    }

    rooms.forEach {
      RoomLink {
        attrs {
          room = it
        }
        +" - ${it.owner}"
        it.sessions.forEach { session ->
          p {
            +session.toString()
          }
        }
      }
    }

    myRooms.forEach { room ->
      RoomLink {
        attrs {
          this.room = room
        }
        +" - "
        button {
          +"Delete"
          attrs {
            onClick = {
              attempt {
                doGet<Unit>("room", room.name, "delete", token = jwt)
              }
            }
          }
        }
        if (room.sessions.none { it.closed == null }) {
          button {
            +"Open"
            attrs {
              onClick = {
                attempt {
                  doGet<Unit>("room", room.name, "newSession", token = jwt)
                }
              }
            }
          }
        }
        room.sessions.forEach { session ->
          div {
            +session.toString()
            if (session.closed == null) {
              button {
                +"Close"
                attrs {
                  onClick = {
                    attempt {
                      doGet("session", session.id.toString(), "close", token = jwt)
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    button {
      +"Create Room"
      attrs {
        onClick = {
          window.prompt("Room name", default = "")?.takeUnless(String::isEmpty)?.let {
            attempt {
              doGet<Unit>("room", it, "create", token = jwt)
            }
          }
        }
      }
    }
  }

/*
  p {
    button {
      attrs {
        onClick = {
          window.location.hash = Routes.welcome
        }
      }
      +"Welcome!"
    }
  }
*/
}