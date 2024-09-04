package com.gu.pandomainauth.service

import java.security.{PrivateKey, PublicKey}

/**
 * This class mainly exists because java.security.KeyPair does not implement a useful `.equals()`` method,
 * and we're going to want to be able to check whether two key-pairs are equal.
 */
case class KeyPair(publicKey: PublicKey, privateKey: PrivateKey)
