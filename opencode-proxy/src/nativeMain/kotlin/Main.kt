import io.ktor.server.application.Application
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import kotlinx.serialization.json.Json
import models.ErrorDetail
import models.ErrorResponse
import routes.configureApiRoutes
import services.OpenCodeService
import services.SessionService

fun main() {
  // Start the server
  embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
    configureRouting()
    println("🚀 OpenCode-OpenAI Proxy Server starting...")
    println("📍 Server: http://0.0.0.0:8080")
    println("🔗 OpenCode API: http://localhost:4096")
    println("✅ Ready for Open WebUI integration")
  }.start(wait = true)
}


fun Application.configureRouting() {
  routing {
    get("/") {
      call.respondText("Hello, world!")
    }
  }
}

