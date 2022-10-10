package org.omgcobra.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.response.*
import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8

private fun getMd5Digest(str: String): ByteArray = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8))
private fun digest(user: String, password: String, realm: String = myRealm) =
  user to getMd5Digest("$user:$realm:$password")

private const val myRealm = "Access to /"

private val userTable = mapOf(
    digest("jetbrains", "foobar"),
    digest("admin", "password"),
    digest("spruitt1", "a")
)

internal val Application.secret get() = "secrettt"
// internal val Application.secret get() = environment.config.property("jwt.secret").getString()
internal val Application.issuer get() = "issuer"
// internal val Application.issuer get() = environment.config.property("jwt.issuer").getString()
internal val Application.audience get() = "audience"
// internal val Application.audience get() = environment.config.property("jwt.audience").getString()
internal val Application.myRealm get() = "realm"
// internal val Application.myRealm get() = environment.config.property("jwt.realm").getString()

fun Application.configureSecurity() {
  authentication {
    jwt("auth-jwt") {
      realm = myRealm
      verifier(JWT.require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .withIssuer(issuer)
        .build())
      validate { credential ->
        when (credential.payload.getClaim("username").asString()) {
          "" -> null
          else -> JWTPrincipal(credential.payload)
        }
      }
    }
  }
}
