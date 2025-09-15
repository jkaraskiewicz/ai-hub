package services

import kotlinx.datetime.Clock
import models.ChatMessage
import utils.base64Encode

class SessionService {
  private val conversationSessions = mutableMapOf<String, String>()

  /**
   * Create a conversation ID based on the first user message content
   * This allows us to identify the same conversation across requests
   */
  fun getConversationId(messages: List<ChatMessage>): String {
    if (messages.isNotEmpty()) {
      val firstUserMessage = messages.first { it.role == "user" }.content
      val hash = base64Encode(firstUserMessage).take(16)
      return "conv_$hash"
    }
    return "conv_${Clock.System.now().epochSeconds}"
  }

  /**
   * Get existing session ID for a conversation or register a new one
   */
  fun getSessionId(conversationId: String): String? {
    return conversationSessions[conversationId]
  }

  /**
   * Register a new session for a conversation
   */
  fun registerSession(conversationId: String, sessionId: String) {
    conversationSessions[conversationId] = sessionId
    println("Registered session $sessionId for conversation $conversationId")
  }

  /**
   * Check if a conversation already has a session
   */
  fun hasSession(conversationId: String): Boolean {
    return conversationSessions.containsKey(conversationId)
  }

  /**
   * Get session count for monitoring
   */
  fun getSessionCount(): Int = conversationSessions.size

  /**
   * Clear expired sessions (could be enhanced with TTL)
   */
  fun clearExpiredSessions() {
    // Simple implementation - could be enhanced with TTL tracking
    // For now, just logging the current state
    println("Active conversations: ${conversationSessions.size}")
  }
}
