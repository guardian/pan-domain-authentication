package com.gu.pandomainauth.service

import cats.Id
import com.google.api.services.directory.DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY
import com.google.auth.oauth2.GoogleCredentials
import com.gu.pandomainauth.internal.DirectoryService

import scala.concurrent.{ExecutionContext, Future, blocking}

trait TwoFactorAuthChecker[F[_]] {
  def check(userEmail: String): F[Boolean]
}

object TwoFactorAuthChecker {
  
  /**
   * Uses the `isEnrolledIn2Sv` field on https://developers.google.com/admin-sdk/directory/reference/rest/v1/users
   * to check the 2FA status of a user.
   * 
   * See also https://github.com/guardian/play-googleauth/pull/204, where this implementation was copied from.
   *
   * @param googleCredentials must have read-only access to retrieve a User using the Admin SDK Directory API
   */
  def blockingTwoFactorAuthChecker(googleCredentials: GoogleCredentials): TwoFactorAuthChecker[Id] = new TwoFactorAuthChecker[Id] {
    private val usersApi = DirectoryService(googleCredentials, ADMIN_DIRECTORY_USER_READONLY).users()
    
    override def check(userEmail: String): Boolean = usersApi.get(userEmail).execute().getIsEnrolledIn2Sv
  }
  
  def wrapBlocking(checker: TwoFactorAuthChecker[Id])(implicit ec: ExecutionContext): TwoFactorAuthChecker[Future] =
    (userEmail: String) => Future { blocking {checker.check(userEmail) } }
}



