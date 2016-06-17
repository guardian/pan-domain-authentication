package com.gu

package object pandomainauth {
  case class PublicKey(key: String) extends AnyVal
  case class PrivateKey(key: String) extends AnyVal
  case class Secret(secret: String) extends AnyVal
}
