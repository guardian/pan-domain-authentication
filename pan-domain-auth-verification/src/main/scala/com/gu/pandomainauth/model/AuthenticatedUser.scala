package com.gu.pandomainauth.model

import java.time.Clock.systemUTC
import java.time.{Clock, Duration, Instant}
import scala.math.Ordering.Implicits._

case class CookieAge(expires: Instant) {
  /**
   * Is the cookie fresh (eg less than 1 hour old) or does it need refreshing?
   */
  def isFresh()(implicit clock: Clock = systemUTC()): Boolean = Instant.now(clock) < expires

  /**
   * Is the cookie age acceptable (ie will we allow the request to succeed) or is it older than even the grace period?
   */
  def isAcceptable(gracePeriod: Duration)(implicit clock: Clock = systemUTC()): Boolean =
    Instant.now(clock) < (expires plus gracePeriod)
}

case class AuthenticatedUser(user: User, authenticatingSystem: String, authenticatedIn: Set[String], expires: Instant, multiFactor: Boolean) {

  val cookieAge: CookieAge = CookieAge(expires)

  def requiringAdditional(system: String): Option[AuthenticatedUser] = Option.when(!authenticatedIn(system))(copy(
    authenticatedIn = authenticatedIn + system
  ))
}
