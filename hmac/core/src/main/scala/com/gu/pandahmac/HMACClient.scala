package com.gu.pandahmac

import com.gu.hmac.HMACHeaders

import java.net.URI

class HMACClient(app: String, stage: String, secretKey: String) extends HMACHeaders {
  def secret: String = secretKey

  def getHMACHeaders(uri: String): Seq[(String, String)] = {
    val headerValues = createHMACHeaderValues(new URI(uri))
    Seq(
      HMACHeaderNames.dateKey -> headerValues.date,
      HMACHeaderNames.hmacKey -> headerValues.token,
      HMACHeaderNames.serviceNameKey -> s"$app-$stage"
    )
  }
}
