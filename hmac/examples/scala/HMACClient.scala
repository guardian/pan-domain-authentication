package example

import java.net.URI
import com.gu.hmac.HMACHeaders

// When using scala you get the `hmac-headers` library and use it directly to generate your HMAC tokens
object HMACClient extends HMACHeaders {
  val secret = "Sanguine, my brother."

  // Unlike the javascript example, with the hmac-headers library you don't provide it a date, it generates one for you
  def makeHMACToken(uri: String): HMACHeaderValues = {
    createHMACHeaderValues(new URI(uri))
  }
}

object ExampleRequestSender {
  def sendRequest = {
    val uri = "/api/examples"
    ws.url("example.com" + uri)
  }
}
