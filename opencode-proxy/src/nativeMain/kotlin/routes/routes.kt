package routes

import io.ktor.server.application.Application
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import services.OpenCodeService2
import utils.TimeProvider

fun Application.configureRouting() {
  val openCodeService2 by inject<OpenCodeService2>()
  val timeProvider by inject<TimeProvider>()

  routing {
    get("/health") {
      call.respond(mapOf("status" to "healthy", "timestamp" to timeProvider.now().toString()))
    }
    route("/v1") {
      get("/models") {

      }
      post("/chat/completions") {

      }
      post("/completions") {

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
