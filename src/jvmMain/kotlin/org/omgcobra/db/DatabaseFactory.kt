package org.omgcobra.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.omgcobra.RoomDTO
import java.net.URI

object Users : IntIdTable() {
  val name = varchar("name", length = 50)
}

object Rooms : IntIdTable() {
  val name = varchar("name", length = 50)
  val user = reference("user", Users)
}

object RoomSessions : IntIdTable() {
  val room = reference("room", Rooms)
  val opened = datetime("opened")
  val closed = datetime("closed")
}

object Messages : IntIdTable() {
  val author = reference("author", Users)
  val session = reference("session", RoomSessions)
  val content = text("content")
  val timestamp = datetime("timestamp")
}

class User(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<User>(Users)

  var name by Users.name
  val rooms by Room referrersOn Rooms.user
  val messages by Message referrersOn Messages.author
}

class Room(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<Room>(Rooms)

  var name by Rooms.name
  var user by User referencedOn Rooms.user

  val dto: RoomDTO get() = RoomDTO(name, user.name)
}

class RoomSession(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<RoomSession>(RoomSessions)

  var room by Room referencedOn RoomSessions.room
  var opened by RoomSessions.opened
  var closed by RoomSessions.closed
}

class Message(id: EntityID<Int>) : IntEntity(id) {
  companion object : IntEntityClass<Message>(Messages)

  var author by User referencedOn Messages.author
  var session by RoomSession referencedOn Messages.session
  var content by Messages.content
  var timestamp by Messages.timestamp
}

object DatabaseFactory {
  fun init() {
    Database.connect(hikari())
    transaction {
      SchemaUtils.create(Users, Rooms, RoomSessions, Messages)
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