package org.omgcobra.room

import kotlinx.browser.window
import kotlinx.html.InputType
import org.omgcobra.api.get
import org.omgcobra.hooks.useLaunch
import org.omgcobra.hooks.useStateWithStorage
import react.*
import react.dom.*
import react.router.dom.RouteComponentProps

val Home: FunctionComponent<RouteComponentProps> = functionComponent(::Home.name) {
  var rooms by useState(setOf<String>())
  var user by useStateWithStorage("name", "Anonymous")

  useLaunch {
    rooms = get("/rooms")
  }

  p {
    +"Home"
  }
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

  p {
    label {
      +"User"
    }
    input(InputType.text) {
      attrs {
        value = user
        name = "user"
        onChange = {
          user = it.target.asDynamic().value as String
        }
      }
    }
  }

  rooms.forEach {
    p {
      a(href = "/room/$it", target = "_blank") {
        +it
      }
    }
  }
}