package com.gu.pandomainauth.service

import java.security.PrivateKey

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.SecurityUtils
import com.google.api.services.admin.directory.model.Groups
import com.google.api.services.admin.directory.{Directory, DirectoryScopes}

import scala.collection.JavaConverters._
import com.gu.pandomainauth.model.{AuthenticatedUser, Google2FAGroupSettings}

class GoogleGroupChecker(directory: Directory) {
  import GroupChecker._

  def checkGroups(authenticatedUser: AuthenticatedUser, groupIds: List[String]): Either[String, Boolean] = {
    if(authenticatedUser.emergency) {
      Right(true)
    } else {
      val query = directory.groups().list().setUserKey(authenticatedUser.user.email)
      if (groupIds.isEmpty) Left("No groups specified.")
      else Right(groupIds.foldLeft(true) { (acc, groupId) => acc & hasGroup(query, groupId) })
    }
  }
}

class Google2FAGroupChecker(directory: Directory, multifactorGroupId: String) {
  import GroupChecker._

  def checkMultifactor(authenticatedUser: AuthenticatedUser): Boolean = {
    val query = directory.groups().list().setUserKey(authenticatedUser.user.email)
    hasGroup(query, multifactorGroupId)
  }
}

object GroupChecker {
  def hasGroup(query: Directory#Groups#List, groupId: String): Boolean = {
    val groupsResponse = query.execute()
    val hasGroupOnPage = Option(groupsResponse.getGroups).exists(_.asScala.exists(_.getEmail == groupId))
    hasGroupOnPage || (if(hasMoreGroups(groupsResponse)) hasGroup( query.setPageToken(groupsResponse.getNextPageToken), groupId ) else false)
  }

  private def hasMoreGroups(groupsResponse: Groups): Boolean = {
    val token = groupsResponse.getNextPageToken
    token != null && token.length > 0
  }

  def buildChecker(settings: Option[Google2FAGroupSettings], bucket: S3Bucket): Option[GoogleGroupChecker] = {
    settings.map { config =>
      new GoogleGroupChecker(buildClient(config, bucket))
    }
  }

  def build2FAChecker(settings: Option[Google2FAGroupSettings], bucket: S3Bucket): Option[Google2FAGroupChecker] = {
    settings.map { config =>
      new Google2FAGroupChecker(buildClient(config, bucket), config.multifactorGroupId)
    }
  }

  private def buildClient(config: Google2FAGroupSettings, bucket: S3Bucket): Directory = {
    val transport = new NetHttpTransport()
    val jsonFactory = new JacksonFactory()

    val credential = new GoogleCredential.Builder()
      .setTransport(transport)
      .setJsonFactory(jsonFactory)
      .setServiceAccountId(config.serviceAccountId)
      .setServiceAccountScopes(List(DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY).asJavaCollection)
      .setServiceAccountUser(config.adminUserEmail)
      .setServiceAccountPrivateKey(loadServiceAccountPrivateKey(config, bucket))
      .build()

    new Directory.Builder(transport, jsonFactory, null)
      .setHttpRequestInitializer(credential).build
  }

  private def loadServiceAccountPrivateKey(config: Google2FAGroupSettings, bucket: S3Bucket): PrivateKey = {
    val certInputStream = bucket.getObjectInputStream(config.serviceAccountCert)
    val serviceAccountPrivateKey = SecurityUtils.loadPrivateKeyFromKeyStore(
      SecurityUtils.getPkcs12KeyStore,
      certInputStream,
      "notasecret", "privatekey", "notasecret"
    )

    try { certInputStream.close() } catch { case _ : Throwable => }

    serviceAccountPrivateKey
  }
}
