package utils

import kotlinx.datetime.Clock

/**
 * Simple base64 encoding for conversation IDs
 */
fun base64Encode(input: String): String {
  // Simple implementation for conversation ID generation
  // In a production app, you'd use a proper base64 encoder
  return input.encodeToByteArray().joinToString("") {
    (it.toInt() and 0xFF).toString(36)
  }
}

/**
 * Generate unique request IDs
 */
fun generateId(prefix: String): String {
  return "$prefix-${Clock.System.now().epochSeconds}"
}

/**
 * Extract text content from OpenCode response parts
 */
fun extractTextFromParts(parts: List<models.MessageResponsePart>): String {
  return parts
    .filter { it.type == "text" }
    .mapNotNull { it.text }
    .joinToString("")
    .ifEmpty { "No response from OpenCode" }
}

/**
 * Estimate token count (simple approximation)
 */
fun estimateTokenCount(text: String): Int {
  // Simple approximation: ~4 characters per token
  return (text.length / 4).coerceAtLeast(1)
}
