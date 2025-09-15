package utils.logger

interface Logger {
  fun log(message: String)
  fun warn(message: String)
  fun error(message: String, throwable: Throwable? = null)
}