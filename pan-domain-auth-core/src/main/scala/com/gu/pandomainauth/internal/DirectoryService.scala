package com.gu.pandomainauth.internal

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.directory.Directory
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

/**
 * See also:
 * https://github.com/guardian/play-googleauth/commit/4e6dde35a46f60c2ea5ebf67c0805390f3c7828a
 */
object DirectoryService {
  def apply(googleCredentials: GoogleCredentials, scope: String): Directory = {
    val credentials = googleCredentials.createScoped(scope)
    val transport = GoogleNetHttpTransport.newTrustedTransport()
    val jsonFactory = GsonFactory.getDefaultInstance
    new Directory.Builder(transport, jsonFactory, new HttpCredentialsAdapter(credentials))
      .setApplicationName("pan-domain-auth").build()
  }
}