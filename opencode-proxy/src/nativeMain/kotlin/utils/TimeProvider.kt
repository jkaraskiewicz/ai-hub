package utils

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class TimeProvider {
  fun now(): Long = Clock.System.now().epochSeconds
}