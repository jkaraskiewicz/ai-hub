package config

data class AppConfig(
  val server: ServerConfig,
  val opencode: OpenCodeConfig
)

data class ServerConfig(
  val host: String = "0.0.0.0",
  val port: Int = 8080
)

data class OpenCodeConfig(
  val baseUrl: String = "http://localhost:4096"
)

object ConfigLoader {
  fun load(): AppConfig {
    return AppConfig(
      server = ServerConfig(
        host = getEnvOrDefault("HOST", "0.0.0.0"),
        port = getEnvOrDefault("PORT", "8080").toInt()
      ),
      opencode = OpenCodeConfig(
        baseUrl = getEnvOrDefault("OPENCODE_API", "http://localhost:4096")
      )
    )
  }

  private fun getEnvOrDefault(key: String, default: String): String {
    // Note: Kotlin/Native environment access is platform-specific
    // For now, using defaults - this would need platform-specific implementation
    return default
  }
}
