@file:OptIn(ExperimentalForeignApi::class)

package utils.logger

import platform.posix.*
import kotlinx.cinterop.*

class NativeLogger : Logger {
  override fun log(message: String) {
    println(message)
  }

  override fun warn(message: String) {
    println(message)
  }

  override fun error(message: String, throwable: Throwable?) {
    val finalMessage = StringBuilder().appendLine(message)
    if (throwable != null) {
      finalMessage.appendLine(throwable.message)
    }
    eprint(finalMessage.toString())
  }

  private companion object {
    val STDERR = fdopen(2, "w")

    fun eprint(message: String) {
      fprintf(STDERR, "%s", message)
      fflush(STDERR)
    }

    fun eprintln(message: String) {
      eprint("$message\n")
    }
  }
}
