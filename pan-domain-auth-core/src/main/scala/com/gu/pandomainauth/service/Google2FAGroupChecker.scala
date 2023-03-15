package com.gu.pandomainauth.service

import com.amazonaws.services.s3.AmazonS3
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.SecurityUtils
import com.google.api.services.directory.Directory
import com.google.api.services.directory.model.Groups
import com.google.api.services.directory.DirectoryScopes
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.ServiceAccountCredentials

import scala.jdk.CollectionConverters._
import com.gu.pandomainauth.model.{AuthenticatedUser, Google2FAGroupSettings}
import org.slf4j.LoggerFactory

class GroupChecker(config: Google2FAGroupSettings, bucketName: String, s3Client: AmazonS3, appName: String) {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val transport = GoogleNetHttpTransport.newTrustedTransport()
  private val jsonFactory = GsonFactory.getDefaultInstance()

  private val credentials = ServiceAccountCredentials.newBuilder()
    .setClientId("not-needed-but-must-not-be-null")
    .setClientEmail(config.serviceAccountId)
    .setScopes(List(DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY).asJavaCollection)
    .setServiceAccountUser(config.adminUserEmail)
    .setPrivateKey(loadServiceAccountPrivateKey)
    .build

  private val httpCredentialsAdapter = new HttpCredentialsAdapter(credentials)

  protected val directory = new Directory.Builder(transport, jsonFactory, httpCredentialsAdapter)
    .setApplicationName(appName)
    .build

  private def loadServiceAccountPrivateKey = {
    val certInputStream = s3Client.getObject(bucketName, config.serviceAccountCert).getObjectContent
    val serviceAccountPrivateKey = SecurityUtils.loadPrivateKeyFromKeyStore(
      SecurityUtils.getPkcs12KeyStore,
      certInputStream,
      "notasecret", "privatekey", "notasecret"
    )

    try { certInputStream.close() } catch { case _ : Throwable => }

    serviceAccountPrivateKey
  }

  private def withGoogle4xxErrorHandling(f: => Boolean): Boolean = {
    try {
      f
    } catch {
      case e: GoogleJsonResponseException if e.getStatusCode >= 400 && e.getStatusCode < 500 =>
        logger.error("Received 4xx error response from Google API", e)
        false
    }
  }

  protected def hasGroup(query: Directory#Groups#List, groupId: String): Boolean =
    withGoogle4xxErrorHandling {
      val groupsResponse = query.execute()
      val hasGroupOnPage = Option(groupsResponse.getGroups).exists(_.asScala.exists(_.getEmail == groupId))
      hasGroupOnPage || (if(hasMoreGroups(groupsResponse)) hasGroup( query.setPageToken(groupsResponse.getNextPageToken), groupId ) else false)
    }

  protected def hasGroup(userEmail: String, groupId: String): Boolean =
    withGoogle4xxErrorHandling {
      directory.members().hasMember(groupId, userEmail).execute().getIsMember()
    }

  private def hasMoreGroups(groupsResponse: Groups): Boolean = {
    val token = groupsResponse.getNextPageToken
    token != null && token.length > 0
  }
}

class GoogleGroupChecker(config: Google2FAGroupSettings, bucketName: String, s3Client: AmazonS3, appName: String) extends GroupChecker(config, bucketName, s3Client, appName) {

  def checkGroups(authenticatedUser: AuthenticatedUser, groupIds: List[String]): Either[String, Boolean] = {
    val query = directory.groups().list().setUserKey(authenticatedUser.user.email)
    if (groupIds.isEmpty) Left("No groups specified.")
    else Right(groupIds.foldLeft(true){(acc, groupId) => acc & hasGroup(query, groupId)})
  }

}

class Google2FAGroupChecker(config: Google2FAGroupSettings, bucketName: String, s3Client: AmazonS3, appName: String) extends GroupChecker(config, bucketName, s3Client, appName) {

  def checkMultifactor(authenticatedUser: AuthenticatedUser): Boolean = {
    hasGroup(authenticatedUser.user.email, config.multifactorGroupId)
  }

}
