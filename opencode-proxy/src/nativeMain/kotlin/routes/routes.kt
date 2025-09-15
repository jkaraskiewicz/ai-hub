package routes

import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import services.OpenCodeService
import services.ProviderService
import utils.TimeProvider
import models.*

fun Application.configureRouting() {
  val openCodeService by inject<OpenCodeService>()
  val providerService by inject<ProviderService>()
  val timeProvider by inject<TimeProvider>()

  routing {
    get("/health") {
      call.respond(mapOf("status" to "healthy", "timestamp" to timeProvider.now().toString()))
    }
    route("/v1") {
      get("/models") {
        try {
          val models = openCodeService.getModels()
          val response = ModelsResponse(data = models)
          call.respond(response)
        } catch (e: Exception) {
          call.respond(
            status = io.ktor.http.HttpStatusCode.InternalServerError,
            message = mapOf("error" to "Failed to fetch models: ${e.message}")
          )
        }
      }
      post("/chat/completions") {
        try {
          val request = call.receive<ChatCompletionRequest>()
          val response = providerService.createChatCompletion(request)
          call.respond(response)
        } catch (e: Exception) {
          call.respond(
            status = io.ktor.http.HttpStatusCode.InternalServerError,
            message = ErrorResponse(
              error = ErrorDetail(
                message = "Failed to create chat completion: ${e.message}",
                type = "chat_completion_error",
                code = "internal_error"
              )
            )
          )
        }
      }
      post("/completions") {
        try {
          val request = call.receive<CompletionRequest>()
          val response = providerService.createCompletion(request)
          call.respond(response)
        } catch (e: Exception) {
          call.respond(
            status = io.ktor.http.HttpStatusCode.InternalServerError,
            message = ErrorResponse(
              error = ErrorDetail(
                message = "Failed to create completion: ${e.message}",
                type = "completion_error",
                code = "internal_error"
              )
            )
          )
        }
      }
      post("/embeddings") {

      }
      get("/files") {

      }
      get("/files/{fileId}") {

      }
    }
  }
}
