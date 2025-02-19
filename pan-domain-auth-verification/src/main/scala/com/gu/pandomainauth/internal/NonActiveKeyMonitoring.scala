package com.gu.pandomainauth.internal

import com.google.common.util.concurrent.RateLimiter
import org.slf4j.LoggerFactory

class NonActiveKeyMonitoring {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val activeFrequency: FrequencyMap[Boolean] = new FrequencyMap[Boolean]()
  private val keyFrequency: FrequencyMap[KeyHashId] = new FrequencyMap[KeyHashId]()

  private val rateLimiter = RateLimiter.create(1.0d / 10)

  def monitor(usedKey: KeyHashId, activeKey: KeyHashId): Unit = {
    val isActive = usedKey == activeKey
    activeFrequency.increment(isActive)
    keyFrequency.increment(usedKey)

    if (!isActive) { // an accepted key, but not the _current_ one - either old or new...
      if (rateLimiter.tryAcquire()) {
        logger.warn(s"NON_ACTIVE_KEY_USED (used=$usedKey, active=$activeKey): active_freq=${activeFrequency.snapshot()} key_freq=${keyFrequency.snapshot()}")
      }
    }
  }
}

object NonActiveKeyMonitoring {
  val instance = new NonActiveKeyMonitoring
}