package com.gu.pandomainauth.model

import java.time.Instant.now
import java.time.{Duration, Instant}
import scala.math.Ordering.Implicits._

case class AuthenticatedUser(user: User, authenticatingSystem: String, authenticatedIn: Set[String], expires: Instant, multiFactor: Boolean) {

  def isExpired = now() > expires
  def isInGracePeriod(period: Duration) = now() < (expires plus period)
}
