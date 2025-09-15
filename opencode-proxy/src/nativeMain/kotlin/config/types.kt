package config

data class AppConfig(
  val serverConfig: HostConfig,
  val clientConfig: HostConfig,
  val apiKeys: ApiKeysConfig,
)

data class ApiKeysConfig(
  val openRouterApiKey: String?,
  val geminiApiKey: String?,
)

data class HostConfig(
  val protocol: Protocol,
  val host: String,
  val port: Int,
)

enum class Protocol(val value: String) {
  HTTP("http"),
  HTTPS("https"),
}
