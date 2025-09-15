package services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import models.*

class OpenCodeService {
  private val httpClient = HttpClient(CIO)
  private val baseUrl = "http://localhost:4096"

  /**
   * Get all available models from OpenCode
   */
  suspend fun getModels(): List<Model> {
    return try {
      val response = httpClient.get("$baseUrl/models") {
        header(HttpHeaders.Accept, ContentType.Application.Json)
      }

      if (response.status.isSuccess()) {
        val modelsOutput: String = response.body()
        parseModelsFromOutput(modelsOutput)
      } else {
        println("Failed to fetch models: ${response.status}")
        emptyList()
      }
    } catch (e: Exception) {
      println("Error fetching models: ${e.message}")
      emptyList()
    }
  }

  /**
   * Create a new session in OpenCode
   */
  suspend fun createSession(title: String): OpenCodeSession {
    val request = CreateSessionRequest(title = title)

    return httpClient.post("$baseUrl/session") {
      contentType(ContentType.Application.Json)
      setBody(request)
    }.body()
  }

  /**
   * Send a message to an OpenCode session
   */
  suspend fun sendMessage(
    sessionId: String,
    message: String,
    model: String
  ): OpenCodeMessageResponse {
    val (providerId, modelId) = parseModelIdentifier(model)

    val request = SendMessageRequest(
      parts = listOf(MessagePart(type = "text", text = message)),
      providerID = providerId,
      modelID = model
    )

    return httpClient.post("$baseUrl/session/$sessionId/message") {
      contentType(ContentType.Application.Json)
      setBody(request)
    }.body()
  }

  /**
   * Parse model identifier to extract provider and model ID
   */
  private fun parseModelIdentifier(model: String): Pair<String, String> {
    return if (model.contains("/")) {
      val parts = model.split("/", limit = 2)
      parts[0] to model
    } else {
      "openrouter" to model
    }
  }

  /**
   * Parse models output from OpenCode CLI
   */
  private fun parseModelsFromOutput(output: String): List<Model> {
    val models = mutableListOf<Model>()
    val currentTime = Clock.System.now().epochSeconds

    output.lines().forEach { line ->
      val trimmedLine = line.trim()
      if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("Available models:")) {
        val ownedBy = when {
          trimmedLine.contains("openrouter/") -> "openrouter"
          trimmedLine.contains("anthropic/") -> "anthropic"
          trimmedLine.contains("google/") -> "google"
          else -> "openrouter"
        }

        models.add(
          Model(
            id = trimmedLine,
            created = currentTime,
            ownedBy = ownedBy
          )
        )
      }
    }

    return models
  }
}
