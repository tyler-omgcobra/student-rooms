package org.omgcobra.plugins

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*

fun Application.configureCORS() {
  install(CORS) {
    method(HttpMethod.Get)
    method(HttpMethod.Post)
    method(HttpMethod.Delete)
    anyHost()
  }
}