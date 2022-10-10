package org.omgcobra.room

import kotlinx.browser.window
import kotlinx.css.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.omgcobra.Chat
import org.omgcobra.Welcome
import org.omgcobra.hooks.useStateWithStorage
import react.*
import react.dom.*
import react.router.dom.*
import styled.*

fun debounce(ms: Int = 100, fn: (dynamic) -> Unit): (dynamic) -> Unit {
  var timer: Int? = null
  return { args ->
    timer?.let { window.clearTimeout(it) }
    timer = window.setTimeout(handler = {
      timer = null
      fn(args)
    }, timeout = ms)
  }
}

object Routes {
  const val home = "/"
  const val page = "/page/:pageId"
  const val chat = "/chat"
  const val welcome = "/welcome"

  fun pageRoute(number: Int) = page.replace(":pageId", "$number")
}

data class Authentication(val username: String = "", val token: String? = null)
val AuthenticationContext = createContext(Authentication())

@ExperimentalSerializationApi
val RoomApp: FunctionComponent<PropsWithChildren> = functionComponent(::RoomApp.name) {
  var user by useStateWithStorage("name", "")
  var token by useState<String?>(null)

  val routes = mapOf<ComponentType<*>, String>(
      Home to Routes.home,
      RoomPage2 to Routes.page,
      Chat to Routes.chat,
      Welcome to Routes.welcome,
  )

  val topLinks = mapOf(
      "Home" to routes[Home],
      "Page 2" to Routes.pageRoute(2),
      "Chat" to routes[Chat],
  )

  AuthenticationContext.Provider(Authentication(user, token)) {
    HashRouter {
      styledDiv {
        css {
          display = Display.flex
          flexDirection = FlexDirection.column
          height = 100.pct
        }
        Login {
          attrs {
            initialUsername = user
            loginHandler = { newUsername, newToken ->
              user = newUsername
              token = newToken
            }
          }
        }
        ul {
          topLinks.forEach {
            li {
              NavLink {
                +it.key
                attrs {
                  to = it.value ?: Routes.home
                  exact = true
                }
              }
            }
          }
        }
        styledDiv {
          css {
            flex(1.0)
            overflow = Overflow.auto
          }
          Switch {
            routes.forEach {
              Route {
                attrs {
                  path = arrayOf(it.value)
                  exact = true
                  component = it.key
                }
              }
            }
          }
        }
      }
    }
  }
}

