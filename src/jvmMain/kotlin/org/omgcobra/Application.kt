package org.omgcobra

import io.ktor.locations.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.ExperimentalSerializationApi
import org.omgcobra.db.DatabaseFactory
import org.omgcobra.plugins.*

@ExperimentalSerializationApi
@KtorExperimentalLocationsAPI
fun main() {
  embeddedServer(Netty, port = System.getenv("PORT").toIntOrNull() ?: 8080) {
    DatabaseFactory.init()

    configureCompression()
    configureCORS()
    configureSecurity()
    configureSockets()
    configureRouting()
    configureSerialization()
  }.start(wait = true)
}
