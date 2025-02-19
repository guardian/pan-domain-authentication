package com.gu.pandomainauth.internal

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.LongAdder
import scala.jdk.CollectionConverters._

/**
 * Used for counting in realtime how often things occur (eg specific public keys are used)
 * The class is thread-safe for concurrent updates.
 */
class FrequencyMap[K] {
  private val freqs: ConcurrentHashMap[K, LongAdder] = new ConcurrentHashMap[K, LongAdder]()

  def increment(k: K): Unit = freqs.computeIfAbsent(k, _ => new LongAdder()).increment()

  def snapshot(): Map[K, Long] = freqs.asScala.mapValues(_.sum).toMap
}
