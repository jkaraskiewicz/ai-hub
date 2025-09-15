package models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// OpenCode API Models
@Serializable
data class OpenCodeProvidersResponse(
  val providers: List<OpenCodeProvider>,
  val default: Map<String, String>,
)

@Serializable
data class OpenCodeProvider(
  val name: String,
  val models: Map<String, OpenCodeModel>,
)

@Serializable
data class OpenCodeModel(
  val id: String,
  val name: String,
  val attachment: Boolean,
  val reasoning: Boolean,
  val temperature: Boolean,
  @SerialName("tool_call") val toolCall: Boolean,
  val modalities: OpenCodeModalities,
  val cost: OpenCodeCost,
  val limit: OpenCodeLimit,
)

@Serializable
data class OpenCodeModalities(
  val input: List<String>,
  val output: List<String>,
)

@Serializable
data class OpenCodeCost(
  val input: Double,
  val output: Double,
  @SerialName("cache_read") val cacheRead: Double? = null,
  @SerialName("cache_write") val cacheWrite: Double? = null,
)

@Serializable
data class OpenCodeLimit(
  val context: Int,
  val output: Int,
)