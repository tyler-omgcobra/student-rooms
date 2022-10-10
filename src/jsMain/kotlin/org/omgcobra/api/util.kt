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

suspend inline fun <reified T> doGet(vararg paths: String, token: String? = null, builder: HttpRequestBuilder.() -> Unit = {}) =
  jsonClient.get<T>(createPath(*paths)) {
    builder()
    token?.let(::bearerToken)
  }

suspend inline fun <reified T> doPost(body: Any, vararg paths: String, token: String? = null, builder: HttpRequestBuilder.() -> Unit = {}): T =
  jsonClient.post(createPath(*paths)) {
    builder()
    contentType(ContentType.Application.Json)
    this.body = body
    token?.let(::bearerToken)
  }

fun HttpRequestBuilder.bearerToken(token: String) {
  headers {
    append(HttpHeaders.Authorization, "Bearer $token")
  }
}

suspend fun websocket(vararg path: String, block: suspend DefaultClientWebSocketSession.() -> Unit) = jsonClient.webSocket(request = {
  url {
    takeFrom(endpoint)
    protocol = when (protocol) {
      URLProtocol.HTTPS -> URLProtocol.WSS
      else -> URLProtocol.WS
    }
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
  } catch (_: ClosedReceiveChannelException) {
  } catch (e: Exception) {
    onFail(e)
  } finally {
    onClose()
  }
}
