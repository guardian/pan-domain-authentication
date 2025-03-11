package com.gu.pandahmac

import com.gu.hmac.HMACHeaders

import java.net.URI

/**
 * This class is used across several Guardian projects: `flexible-content`, `workflow`, `typerighter`, etc.
 *
 * https://github.com/guardian/flexible-content/blob/8ba8aa06884e593d73e8ace77a059fdf912018ce/flexible-content-apiv2/src/main/scala/com/gu/flexiblecontent/apiv2/management/HMACClient.scala#L8
 * https://github.com/guardian/workflow/blob/6c6a44bf5e11a4d92be3fdd9b746ba61bfbd4702/prole/app/lib/client/HMACClient.scala#L7
 * https://github.com/guardian/typerighter/blob/f1f97950f1e713088cd5147dbfdf65c693a5237d/apps/common-lib/src/main/scala/com/gu/typerighter/lib/HMACClient.scala#L8
 */
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
