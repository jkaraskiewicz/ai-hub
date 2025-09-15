package di

import config.AppConfig
import config.ApiKeysConfig
import config.HostConfig
import config.Protocol
import platform.posix.getenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import services.OpenCodeService
import services.ProviderService
import utils.TimeProvider
import utils.logger.Logger
import utils.logger.NativeLogger

val applicationModule = module {
  single<AppConfig> { defaultAppConfig }
}

val utilsModule = module {
  single<Logger> { NativeLogger() }
  single<TimeProvider> { TimeProvider() }
}

val httpModule = module {
  single<HttpClient> {
    HttpClient(CIO) {
      install(ContentNegotiation) {
        json(Json {
          ignoreUnknownKeys = true
        })
      }
    }
  }
  single<OpenCodeService> { OpenCodeService(get(), get(), get(), get()) }
  single<ProviderService> { ProviderService(get(), get(), get()) }
}

private val defaultAppConfig = AppConfig(
  serverConfig = HostConfig(
    protocol = Protocol.HTTP,
    host = "0.0.0.0",
    port = 8080,
  ),
  clientConfig = HostConfig(
    protocol = Protocol.HTTP, host = "localhost", port = 4096
  ),
  apiKeys = ApiKeysConfig(
    openRouterApiKey = getenv("OPENROUTER_API_KEY")?.toKString(),
    geminiApiKey = getenv("GEMINI_API_KEY")?.toKString(),
  ),
)