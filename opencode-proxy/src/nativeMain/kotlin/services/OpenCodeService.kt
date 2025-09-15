package services

import config.AppConfig
import config.toUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import models.Model
import utils.TimeProvider
import utils.logger.Logger

class OpenCodeService(
  private val httpClient: HttpClient,
  private val appConfig: AppConfig,
  private val timeProvider: TimeProvider,
  private val logger: Logger,
) {
  suspend fun getModels(): List<Model> {
    val baseUrl = appConfig.clientConfig.toUrl()
    return try {
      val response = httpClient.get("$baseUrl/models") {
        header(HttpHeaders.Accept, ContentType.Application.Json)
      }
      TODO("Continue!")
    } catch (e: Exception) {
      logger.error("Error fetching models", e)
      emptyList()
    }
  }
}