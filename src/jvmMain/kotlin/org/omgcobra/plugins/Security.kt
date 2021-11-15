package org.omgcobra.plugins

import io.ktor.application.*
import io.ktor.auth.*
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

private fun getMd5Digest(str: String): ByteArray = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8))
private fun digest(user: String, password: String, realm: String = myRealm) =
  user to getMd5Digest("$user:$realm:$password")

private const val myRealm = "Access to /"

private val userTable = mapOf(
    digest("jetbrains", "foobar"),
    digest("admin", "password")
)

fun Application.configureSecurity() {
  authentication {
    basic("auth-basic") {
      validate { UserIdPrincipal(it.name) }
    }
    digest("auth-digest") {
      realm = myRealm
      digestProvider { userName, _ -> userTable[userName] }
    }
  }
}
