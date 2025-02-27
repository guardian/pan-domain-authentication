package com.gu.pandomainauth.internal

import com.google.common.hash.Hashing

import java.security.PublicKey

case class KeyHashId(id: String)

object KeyHashId {
  // Avoid collisions when used with hourly new keys over the course of a year
  val NumberOfDigitsNecessaryToAvoidBirthdayCollisions: Int = 5

  def calculateFor(publicKey: PublicKey): KeyHashId = new KeyHashId(
    BigInt(Hashing.sha256().hashBytes(publicKey.getEncoded).asBytes()).abs.toString(36)
      .take(NumberOfDigitsNecessaryToAvoidBirthdayCollisions)
  )
}