package services

import config.AppConfig
import config.toUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.datetime.Clock
import models.*
import utils.TimeProvider
import utils.logger.Logger
import utils.generateId
import utils.estimateTokenCount
import converters.toOpenAIModel

class OpenCodeService(
  private val httpClient: HttpClient,
  private val appConfig: AppConfig,
  private val timeProvider: TimeProvider,
  private val logger: Logger,
) {
  suspend fun getModels(): List<Model> {
    val baseUrl = appConfig.clientConfig.toUrl()
    return try {
      logger.log("Fetching models from OpenCode at $baseUrl/config/providers")

      val response = httpClient.get("$baseUrl/config/providers") {
        header(HttpHeaders.Accept, ContentType.Application.Json)
      }

      val providersResponse: OpenCodeProvidersResponse = response.body()

      // Convert all OpenCode models to OpenAI-compatible models
      val models = mutableListOf<Model>()

      providersResponse.providers.forEach { provider ->
        provider.models.values.forEach { openCodeModel ->
          val openAIModel = openCodeModel.toOpenAIModel(provider)
          models.add(openAIModel)
        }
      }

      logger.log("Successfully converted ${models.size} models from ${providersResponse.providers.size} providers")
      models

    } catch (e: Exception) {
      logger.error("Error fetching models from OpenCode", e)
      emptyList()
    }
  }

  suspend fun createCompletion(request: CompletionRequest): CompletionResponse {
    val baseUrl = appConfig.clientConfig.toUrl()

    return try {
      logger.log("Creating completion for model ${request.model}")

      // Convert OpenAI completion request to OpenCode chat format
      val openCodeRequest = mapOf(
        "model" to request.model,
        "messages" to listOf(
          mapOf("role" to "user", "content" to request.prompt)
        ),
        "stream" to request.stream,
        "max_tokens" to request.maxTokens,
        "temperature" to request.temperature
      )

      val response = httpClient.post("$baseUrl/v1/chat/completions") {
        header(HttpHeaders.Accept, ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(openCodeRequest)
      }

      val chatResponse: ChatCompletionResponse = response.body()

      // Convert chat response to completion response
      val completionChoices = chatResponse.choices.map { choice ->
        CompletionChoice(
          index = choice.index,
          text = choice.message?.content ?: choice.delta?.content ?: "",
          logprobs = null,
          finishReason = choice.finishReason ?: "stop"
        )
      }

      CompletionResponse(
        id = generateId("cmpl"),
        created = Clock.System.now().epochSeconds,
        model = request.model,
        choices = completionChoices,
        usage = chatResponse.usage
      )

    } catch (e: Exception) {
      logger.error("Error creating completion", e)
      throw e
    }
  }

  suspend fun createChatCompletion(request: ChatCompletionRequest): ChatCompletionResponse {
    val baseUrl = appConfig.clientConfig.toUrl()

    return try {
      logger.log("Creating chat completion for model ${request.model}")

      val response = httpClient.post("$baseUrl/v1/chat/completions") {
        header(HttpHeaders.Accept, ContentType.Application.Json)
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(request)
      }

      response.body()

    } catch (e: Exception) {
      logger.error("Error creating chat completion", e)
      throw e
    }
  }
}