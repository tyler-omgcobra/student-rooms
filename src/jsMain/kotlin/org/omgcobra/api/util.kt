package org.omgcobra.api

import io.ktor.client.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.browser.window
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive

val endpoint = window.location.origin

val jsonClient = HttpClient {
  install(JsonFeature) {
    serializer = KotlinxSerializer()
  }
  install(WebSockets)
}

fun createPath(vararg paths: String) = listOf(endpoint, *paths).joinToString("/")

suspend inline fun <reified T> get(vararg paths: String, builder: HttpRequestBuilder.() -> Unit = {}) =
  jsonClient.get<T>(createPath(*paths), builder)

suspend inline fun <reified T> post(body: Any, vararg paths: String): T =
  jsonClient.post(createPath(*paths)) {
    contentType(ContentType.Application.Json)
    this.body = body
  }

suspend fun websocket(vararg path: String, block: suspend DefaultClientWebSocketSession.() -> Unit) = jsonClient.webSocket(request = {
  url {
    takeFrom(endpoint)
    protocol = URLProtocol.WS
    path(*path)
  }
}, block = block)

suspend fun ws(
    vararg path: String,
    onOpen: suspend DefaultClientWebSocketSession.() -> Unit = {},
    onClose: suspend () -> Unit = {},
    onFail: suspend (e: Exception) -> Unit = {},
    block: suspend DefaultClientWebSocketSession.(String) -> Unit
) = websocket(*path) {
  try {
    onOpen()
    while (isActive) {
      when (val frame = incoming.receive()) {
        is Frame.Text -> block(frame.readText())
      }
    }
  } catch (e: ClosedReceiveChannelException) {
  } catch (e: Exception) {
    onFail(e)
  } finally {
    onClose()
  }
}
