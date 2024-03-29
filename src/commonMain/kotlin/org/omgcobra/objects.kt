package org.omgcobra

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.*

@Serializable
sealed class ChatEvent(val type: String)

@Serializable
@SerialName("message")
data class ChatMessage(val text: String = "", val author: String = "") : ChatEvent("message")
@Serializable
@SerialName("joinChat")
data class JoinChat(val previousMessages: List<ChatMessage>, val author: String) : ChatEvent("joinChat")
@Serializable
@SerialName("participantsUpdate")
data class UpdateParticipants(val participants: Set<String>) : ChatEvent("participantsUpdate")

@Serializable
data class RoomDTO(val name: String, val owner: String, val sessions: List<RoomSessionDTO>)

@Serializable
data class RoomSessionDTO(val id: Int, val opened: LocalDateTime, val closed: LocalDateTime?)

@Serializable
data class UserCredential(val username: String, val password: String)