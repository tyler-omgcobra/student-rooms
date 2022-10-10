package org.omgcobra.room

import org.omgcobra.RoomDTO
import react.*
import react.dom.*

external interface RoomProps : PropsWithChildren {
  var room: RoomDTO
}

val RoomLink: FunctionComponent<RoomProps> = functionComponent(::RoomLink.name) { props ->
  p {
    a(href = "/room/${props.room.name}", target = "_blank") {
      +props.room.name
    }

    props.children()
  }
}