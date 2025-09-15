import config.AppConfig
import config.toUrl
import di.applicationModule
import di.httpModule
import di.utilsModule
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import routes.configureRouting
import utils.logger.Logger

fun main() {
  embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
    configureDI()
    configureRouting()
    printStatus()
  }.start(wait = true)
}

private fun Application.configureDI() {
  install(Koin) {
    modules(applicationModule, utilsModule, httpModule)
  }
}

private fun Application.printStatus() {
  val appConfig = get<AppConfig>()
  get<Logger>().run {
    log("🚀 OpenCode-OpenAI Proxy Server starting...")
    log("📍 Server: ${appConfig.serverConfig.toUrl()}")
    log("🔗 OpenCode API: ${appConfig.clientConfig.toUrl()}")
    log("✅ Ready for Open WebUI integration")
  }
}