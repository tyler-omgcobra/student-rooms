package org.omgcobra.room

import kotlinx.browser.window
import react.FunctionComponent
import react.dom.*
import react.functionComponent
import react.router.dom.RouteComponentProps

val RoomPage2: FunctionComponent<RouteComponentProps> = functionComponent(::RoomPage2.name) { props ->
  val pageId = props.match.params["pageId"]?.toIntOrNull() ?: 0
  button {
    attrs {
      onClick = {
        window.location.hash = Routes.pageRoute(pageId - 1)
      }
    }
    +"Prev"
  }
  +"Page $pageId"
  button {
    attrs {
      onClick = {
        window.location.hash = Routes.pageRoute(pageId + 1)
      }
    }
    +"Next"
  }
}