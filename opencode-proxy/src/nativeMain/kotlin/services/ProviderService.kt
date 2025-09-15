package services

import config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.datetime.Clock
import models.*
import utils.generateId
import utils.logger.Logger

class ProviderService(
  private val httpClient: HttpClient,
  private val appConfig: AppConfig,
  private val logger: Logger,
) {
  suspend fun createChatCompletion(request: ChatCompletionRequest): ChatCompletionResponse {
    return when {
      request.model.startsWith("openrouter/") || request.model.contains("/") -> {
        routeToOpenRouter(request)
      }
      request.model.startsWith("gemini") -> {
        routeToGemini(request)
      }
      else -> {
        // Default to OpenRouter for unknown models
        routeToOpenRouter(request)
      }
    }
  }

  suspend fun createCompletion(request: CompletionRequest): CompletionResponse {
    // Convert completion to chat completion for provider APIs
    val chatRequest = ChatCompletionRequest(
      model = request.model,
      messages = listOf(
        ChatMessage(role = "user", content = request.prompt)
      ),
      maxTokens = request.maxTokens,
      temperature = request.temperature,
      stream = request.stream
    )

    val chatResponse = createChatCompletion(chatRequest)

    // Convert back to completion response
    val completionChoices = chatResponse.choices.map { choice ->
      CompletionChoice(
        index = choice.index,
        text = choice.message?.content ?: choice.delta?.content ?: "",
        logprobs = null,
        finishReason = choice.finishReason ?: "stop"
      )
    }

    return CompletionResponse(
      id = generateId("cmpl"),
      created = Clock.System.now().epochSeconds,
      model = request.model,
      choices = completionChoices,
      usage = chatResponse.usage
    )
  }

  private suspend fun routeToOpenRouter(request: ChatCompletionRequest): ChatCompletionResponse {
    val apiKey = appConfig.apiKeys.openRouterApiKey
      ?: throw IllegalStateException("OpenRouter API key not configured")

    logger.log("Routing to OpenRouter for model ${request.model}")

    val response = httpClient.post("https://openrouter.ai/api/v1/chat/completions") {
      header(HttpHeaders.Authorization, "Bearer $apiKey")
      header(HttpHeaders.ContentType, ContentType.Application.Json)
      header("HTTP-Referer", "https://github.com/jkaraskiewicz/ai-hub")
      header("X-Title", "OpenCode AI Hub")
      setBody(request)
    }

    return response.body()
  }

  private suspend fun routeToGemini(request: ChatCompletionRequest): ChatCompletionResponse {
    val apiKey = appConfig.apiKeys.geminiApiKey
      ?: throw IllegalStateException("Gemini API key not configured")

    logger.log("Routing to Gemini for model ${request.model}")

    // TODO: Implement Gemini API routing when needed
    throw NotImplementedError("Gemini routing not yet implemented")
  }
}