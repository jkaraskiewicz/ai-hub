package models

import kotlinx.serialization.Serializable

@Serializable
data class OpenCodeSession(
  val id: String,
  val version: String,
  val projectID: String,
  val directory: String,
  val title: String,
  val time: SessionTime
)

@Serializable
data class SessionTime(
  val created: Long,
  val updated: Long
)

@Serializable
data class CreateSessionRequest(
  val title: String,
  val parentID: String? = null
)

@Serializable
data class MessagePart(
  val type: String,
  val text: String
)

@Serializable
data class SendMessageRequest(
  val parts: List<MessagePart>,
  val providerID: String,
  val modelID: String
)

@Serializable
data class OpenCodeMessageResponse(
  val info: MessageInfo,
  val parts: List<MessageResponsePart>,
  val blocked: Boolean = false
)

@Serializable
data class MessageInfo(
  val id: String,
  val role: String,
  val sessionID: String,
  val mode: String,
  val cost: Double,
  val tokens: TokenInfo,
  val modelID: String,
  val providerID: String,
  val time: MessageTime
)

@Serializable
data class TokenInfo(
  val input: Int,
  val output: Int,
  val reasoning: Int,
  val cache: CacheTokens
)

@Serializable
data class CacheTokens(
  val write: Int,
  val read: Int
)

@Serializable
data class MessageTime(
  val created: Long,
  val completed: Long
)

@Serializable
data class MessageResponsePart(
  val id: String,
  val messageID: String,
  val sessionID: String,
  val type: String,
  val text: String? = null,
  val time: PartTime? = null,
  val tokens: TokenInfo? = null,
  val cost: Double? = null
)

@Serializable
data class PartTime(
  val start: Long,
  val end: Long
)
