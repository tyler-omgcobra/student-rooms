package org.omgcobra

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.ExperimentalSerializationApi
import org.omgcobra.room.RoomApp
import react.dom.*
import styled.injectGlobal

@ExperimentalSerializationApi
fun main() {
  window.onload = {
    injectGlobal(globalStyles)

    render(document.getElementById("root")) {
      RoomApp {}
    }
  }
}
