package routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import models.*
import services.OpenCodeService
import services.SessionService
import utils.estimateTokenCount
import utils.extractTextFromParts
import utils.generateId

fun Application.configureApiRoutes() {
  val openCodeService = OpenCodeService()
  val sessionService = SessionService()

  routing {
    // Health check
    get("/health") {
      call.respond(mapOf("status" to "healthy", "timestamp" to Clock.System.now().toString()))
    }

    // OpenAI API routes
    route("/v1") {
      // List models
      get("/models") {
        try {
          val models = openCodeService.getModels()
          val response = ModelsResponse(data = models)
          call.respond(response)
        } catch (e: Exception) {
          call.respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse(ErrorDetail("Failed to fetch models", "proxy_error", "model_fetch_failed"))
          )
        }
      }

      // Chat completions
      post("/chat/completions") {
        try {
          val request = call.receive<ChatCompletionRequest>()

          // Get or create session for conversation
          val conversationId = sessionService.getConversationId(request.messages)
          val sessionId = sessionService.getSessionId(conversationId)
            ?: run {
              val newSession = openCodeService.createSession("WebUI Chat - ${Clock.System.now()}")
              sessionService.registerSession(conversationId, newSession.id)
              newSession.id
            }

          // Extract current user message
          val userMessage = request.messages.lastOrNull()?.content ?: ""

          // Send message to OpenCode
          val openCodeResponse = openCodeService.sendMessage(sessionId, userMessage, request.model)

          // Extract response text
          val responseText = extractTextFromParts(openCodeResponse.parts)

          if (request.stream) {
            // Streaming response
            call.response.header(HttpHeaders.ContentType, "text/event-stream")
            call.response.header(HttpHeaders.CacheControl, "no-cache")
            call.response.header(HttpHeaders.Connection, "keep-alive")

            val streamResponse = ChatCompletionResponse(
              id = generateId("chatcmpl"),
              created = Clock.System.now().epochSeconds,
              model = request.model,
              choices = listOf(
                ChatChoice(
                  index = 0,
                  delta = ChatMessage(role = "assistant", content = responseText),
                  finishReason = null
                )
              ),
              usage = Usage(
                promptTokens = estimateTokenCount(userMessage),
                completionTokens = estimateTokenCount(responseText),
                totalTokens = estimateTokenCount(userMessage) + estimateTokenCount(responseText)
              )
            )

            call.respondText("data: ${kotlinx.serialization.json.Json.encodeToString(ChatCompletionResponse.serializer(), streamResponse)}\\n\\n")
            call.respondText("data: [DONE]\\n\\n")
          } else {
            // Non-streaming response
            val response = ChatCompletionResponse(
              id = generateId("chatcmpl"),
              created = Clock.System.now().epochSeconds,
              model = request.model,
              choices = listOf(
                ChatChoice(
                  index = 0,
                  message = ChatMessage(role = "assistant", content = responseText),
                  finishReason = "stop"
                )
              ),
              usage = Usage(
                promptTokens = estimateTokenCount(userMessage),
                completionTokens = estimateTokenCount(responseText),
                totalTokens = estimateTokenCount(userMessage) + estimateTokenCount(responseText)
              )
            )

            call.respond(response)
          }
        } catch (e: Exception) {
          call.respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse(ErrorDetail("Chat completion failed: ${e.message}", "opencode_error", "chat_failed"))
          )
        }
      }

      // Text completions
      post("/completions") {
        try {
          val request = call.receive<CompletionRequest>()

          // Create simple conversation for completions
          val fakeMessages = listOf(ChatMessage(role = "user", content = request.prompt))
          val conversationId = sessionService.getConversationId(fakeMessages)
          val sessionId = sessionService.getSessionId(conversationId)
            ?: run {
              val newSession = openCodeService.createSession("WebUI Completions - ${Clock.System.now()}")
              sessionService.registerSession(conversationId, newSession.id)
              newSession.id
            }

          // Send prompt to OpenCode
          val openCodeResponse = openCodeService.sendMessage(sessionId, request.prompt, request.model)
          val responseText = extractTextFromParts(openCodeResponse.parts)

          val response = CompletionResponse(
            id = generateId("cmpl"),
            created = Clock.System.now().epochSeconds,
            model = request.model,
            choices = listOf(
              CompletionChoice(
                index = 0,
                text = responseText,
                finishReason = "stop"
              )
            ),
            usage = Usage(
              promptTokens = estimateTokenCount(request.prompt),
              completionTokens = estimateTokenCount(responseText),
              totalTokens = estimateTokenCount(request.prompt) + estimateTokenCount(responseText)
            )
          )

          call.respond(response)
        } catch (e: Exception) {
          call.respond(
            HttpStatusCode.InternalServerError,
            ErrorResponse(ErrorDetail("Completion failed: ${e.message}", "opencode_error", "completion_failed"))
          )
        }
      }

      // Embeddings (not supported)
      post("/embeddings") {
        call.respond(
          HttpStatusCode.NotImplemented,
          ErrorResponse(ErrorDetail(
            "Embeddings not supported by OpenCode. Consider using a dedicated embedding service.",
            "not_implemented",
            "embeddings_not_supported"
          ))
        )
      }

      // Files endpoints (basic implementation)
      get("/files") {
        call.respond(ModelsResponse(data = emptyList())) // Empty file list for now
      }

      get("/files/{fileId}") {
        val fileId = call.parameters["fileId"]
        call.respond(
          HttpStatusCode.NotImplemented,
          ErrorResponse(ErrorDetail(
            "File operations not fully implemented",
            "not_implemented",
            "files_not_supported"
          ))
        )
      }
    }
  }
}
