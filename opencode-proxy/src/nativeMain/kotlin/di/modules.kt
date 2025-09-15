package di

import config.AppConfig
import config.HostConfig
import config.Protocol
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.koin.dsl.module
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
  single<HttpClient> { HttpClient(CIO) }
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
)