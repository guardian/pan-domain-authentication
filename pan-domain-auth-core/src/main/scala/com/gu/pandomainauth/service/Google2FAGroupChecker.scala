package com.gu.pandomainauth.service

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.SecurityUtils
import com.google.api.services.admin.directory.model.Groups
import com.google.api.services.admin.directory.{Directory, DirectoryScopes}

import scala.collection.JavaConverters._
import com.gu.pandomainauth.model.{AuthenticatedUser, Google2FAGroupSettings}

class Google2FAGroupChecker(config: Google2FAGroupSettings, bucket: S3Bucket) {

  val transport = new NetHttpTransport()
  val jsonFactory = new JacksonFactory()

  val credential = new GoogleCredential.Builder()
    .setTransport(transport)
    .setJsonFactory(jsonFactory)
    .setServiceAccountId(config.serviceAccountId)
    .setServiceAccountScopes(List(DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY).asJavaCollection)
    .setServiceAccountUser(config.adminUserEmail)
    .setServiceAccountPrivateKey(loadServiceAccountPrivateKey)
    .build()

  val directory = new Directory.Builder(transport, jsonFactory, null)
    .setHttpRequestInitializer(credential).build


  def checkMultifactor(authenticatedUser: AuthenticatedUser) = {

    val query = directory.groups().list().setUserKey(authenticatedUser.user.email)
    has2FAGroup(query)
  }

  private def has2FAGroup(query: Directory#Groups#List): Boolean = {
    val groupsResponse = query.execute()
    val hasGroupOnPage = Option(groupsResponse.getGroups()).map(_.asScala.exists(_.getEmail == config.multifactorGroupId)).getOrElse(false)
    hasGroupOnPage || (if(hasMoreGroups(groupsResponse)) has2FAGroup( query.setPageToken(groupsResponse.getNextPageToken()) ) else false)
  }

  private def hasMoreGroups(groupsResponse: Groups) = {
    val token = groupsResponse.getNextPageToken

    token != null && token.length > 0
  }

  private def loadServiceAccountPrivateKey = {
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
