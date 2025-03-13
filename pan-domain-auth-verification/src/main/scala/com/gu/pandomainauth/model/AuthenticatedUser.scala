package com.gu.pandomainauth.model

import java.time.Instant.now
import java.time.{Duration, Instant}
import scala.math.Ordering.Implicits._

case class AuthenticatedUser(user: User, authenticatingSystem: String, authenticatedIn: Set[String], expires: Instant, multiFactor: Boolean) {

  // **WIP thinking**
  //
  // The following structure might be better:
  // case class CookieStatus(isFresh: Boolean, isAcceptable: Boolean)
  // issued ------------------------- fresh period over -------------------- grace period over
  // [--working-and-will-not-renew--------][---working-and-will-renew--------------------------][--not-working---]

  def isExpired = now() > expires
  def isInGracePeriod(period: Duration) = now() < (expires plus period)
}
