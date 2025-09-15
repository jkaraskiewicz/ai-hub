package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Model(
  val id: String,
  val `object`: String = "model",
  val created: Long,
  @SerialName("owned_by") val ownedBy: String
)

@Serializable
data class ModelsResponse(
  val `object`: String = "list",
  val data: List<Model>
)

@Serializable
data class ChatMessage(
  val role: String,
  val content: String
)

@Serializable
data class ChatCompletionRequest(
  val model: String,
  val messages: List<ChatMessage>,
  val stream: Boolean = false,
  @SerialName("max_tokens") val maxTokens: Int? = null,
  val temperature: Double? = null
)

@Serializable
data class ChatCompletionResponse(
  val id: String,
  val `object`: String = "chat.completion",
  val created: Long,
  val model: String,
  val choices: List<ChatChoice>,
  val usage: Usage
)

@Serializable
data class ChatChoice(
  val index: Int,
  val message: ChatMessage? = null,
  val delta: ChatMessage? = null,
  @SerialName("finish_reason") val finishReason: String?
)

@Serializable
data class Usage(
  @SerialName("prompt_tokens") val promptTokens: Int,
  @SerialName("completion_tokens") val completionTokens: Int,
  @SerialName("total_tokens") val totalTokens: Int
)

@Serializable
data class CompletionRequest(
  val model: String,
  val prompt: String,
  val stream: Boolean = false,
  @SerialName("max_tokens") val maxTokens: Int? = null,
  val temperature: Double? = null
)

@Serializable
data class CompletionResponse(
  val id: String,
  val `object`: String = "text_completion",
  val created: Long,
  val model: String,
  val choices: List<CompletionChoice>,
  val usage: Usage
)

@Serializable
data class CompletionChoice(
  val index: Int,
  val text: String,
  val logprobs: String? = null,
  @SerialName("finish_reason") val finishReason: String
)

@Serializable
data class ErrorResponse(
  val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
  val message: String,
  val type: String,
  val code: String
)
