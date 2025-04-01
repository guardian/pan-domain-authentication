package com.gu.pandomainauth.internal.planning

/**
 * A Panda-specific response to an HTTP request
 * 
 * [[Plan]] models the response at the point _before_ translating & signing the data into cookie payloads etc,
 * making it easier to write tests that check that [[Plan]]s are correct.
 *
 * @param respType dictates the nature of the response - allowing access, or withholding access (redirecting, bad-request, etc)
 * @param respMod  any modifications that should be made to the response, even if that response isn't
 *                 _generated_ by Panda (ie a _normal_ page response for an authorised user).
 *                 There must exist a way to translate the RespMod into the simple concrete
 *                 fields of a [[ResponseModification]] (ie cookie values and response headers).
 */
case class Plan[RespType, RespMod](respType: RespType, respMod: Option[RespMod] = None)
