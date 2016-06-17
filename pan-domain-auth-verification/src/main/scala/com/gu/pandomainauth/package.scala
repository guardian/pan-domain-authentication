package com.gu

package object pandomainauth {
  case class PublicKey(key: String)
  case class PrivateKey(key: String)
  case class Secret(secret: String)
}
