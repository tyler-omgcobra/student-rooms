package org.omgcobra

import io.ktor.locations.*
import io.ktor.server.engine.*
import io.ktor.server.tomcat.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.omgcobra.plugins.*

@ExperimentalSerializationApi
@KtorExperimentalLocationsAPI
fun main() {
  embeddedServer(Tomcat, port = 8080, host = "0.0.0.0") {
    configureCompression()
    configureCORS()
    configureSecurity()
    configureSockets()
    configureRouting()
    configureSerialization()
  }.start(wait = true)
}
