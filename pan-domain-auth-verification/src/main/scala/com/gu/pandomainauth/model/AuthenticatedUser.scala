package com.gu.pandomainauth.model

import java.time.{Duration, Instant}
import scala.math.Ordering.Implicits._

case class AuthenticatedUser(user: User, authenticatingSystem: String, authenticatedIn: Set[String], expires: Instant, multiFactor: Boolean) {

  def isExpired = Instant.now() > expires
  def isInGracePeriod(period: Duration) = Instant.now() < (expires plus period)
}
