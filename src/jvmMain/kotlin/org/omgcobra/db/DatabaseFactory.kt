package org.omgcobra.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI

object Users : IntIdTable() {
  val name = varchar("name", length = 50)
}

object Rooms : IntIdTable() {
  val name = varchar("name", length = 50)
  val user = reference("user", Users)
}

class User(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<User>(Users)

  var name by Users.name
  val rooms by Room referrersOn Rooms.user
}

class Room(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<Room>(Rooms)

  var name by Rooms.name
  var user by User referencedOn Rooms.user
}

object DatabaseFactory {
  fun init() {
    Database.connect(hikari())
    transaction {
      SchemaUtils.create(Users, Rooms)
    }
  }

  private fun hikari(): HikariDataSource = HikariDataSource(HikariConfig().apply {
    driverClassName = System.getenv("JDBC_DRIVER")
    val dbUri = URI(System.getenv("DATABASE_URL"))
    val (username, password) = dbUri.userInfo.split(":")
    jdbcUrl = "jdbc:postgresql://${dbUri.host}:${dbUri.port}${dbUri.path}" +
        "?sslmode=require&user=$username&password=$password"
    maximumPoolSize = 3
    isAutoCommit = false
    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    validate()
  })
}