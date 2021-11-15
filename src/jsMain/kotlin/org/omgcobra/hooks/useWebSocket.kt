package org.omgcobra.hooks

import io.ktor.client.features.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.omgcobra.api.ws
import react.*

fun useWebSocket(
    vararg path: String,
    retryMillis: Long = 5000,
    onOpen: suspend DefaultClientWebSocketSession.() -> Unit = {},
    onClose: suspend () -> Unit = {},
    onFail: suspend (e: Exception) -> Unit = {},
    block: suspend DefaultClientWebSocketSession.(String) -> Unit
): MutableRefObject<DefaultClientWebSocketSession> {
  val session = useRef<DefaultClientWebSocketSession>(null)

  useLaunch(message = "WebSocket cleanup") {
    while (isActive) {
      ws(*path, onOpen = {
        session.current = this
        onOpen()
      }, onClose = {
        session.current = null
        onClose()
      }, onFail = {
        println("${it::class.simpleName}: ${it.message}")
        onFail(it)
      }, block = block)

      delay(retryMillis)
    }
  }

  return session
}
