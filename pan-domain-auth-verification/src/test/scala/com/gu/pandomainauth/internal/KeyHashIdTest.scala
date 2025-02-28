package com.gu.pandomainauth.internal

import com.gu.pandomainauth.SampleConf.loadExample
import com.gu.pandomainauth.internal.KeyHashId.calculateFor
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class KeyHashIdTest extends AnyFlatSpec with Matchers {

  val rotationUpcoming = loadExample("1.rotation-upcoming")
  val rotationInProgress = loadExample("2.rotation-in-progress")


  val oldKey = KeyHashId("tudwv")
  val newKey = KeyHashId("povb6")

  "key hash ids" should "be stable so that we can use them as a simple text identifier for public keys" in {
    rotationUpcoming.acceptedPublicKeys.map(calculateFor) shouldBe Seq(oldKey, newKey)

    newKey shouldNot be(oldKey)
    newKey.id shouldNot startWith(oldKey.id.take(1)) // the hashCode on PublicKey seems bad enough that this happens

    calculateFor(rotationUpcoming.activePublicKey) shouldBe oldKey
    rotationUpcoming.alsoAccepted.map(calculateFor) shouldBe Seq(newKey)

    calculateFor(rotationInProgress.activePublicKey) shouldBe newKey
    rotationInProgress.alsoAccepted.map(calculateFor) shouldBe Seq(oldKey)
  }
}
