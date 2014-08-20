package com.gu.pandomainauth.service

import scala.collection.JavaConverters._
import com.gu.pandomainauth.model.{AuthenticatedUser, Google2FAGroupSettings}
import com.google.gdata.client.appsforyourdomain.AppsGroupsService

class Google2FAGroupChecker(config: Google2FAGroupSettings) {

  val appsGroupsService =
    new AppsGroupsService(
      config.googleUser,
      config.googlePassword,
      "guardian.co.uk",
      ""
    )

  def getGroupIds(memberName: String): Set[String] =
    appsGroupsService.retrieveGroups(memberName, false).getEntries.asScala.flatMap { entry =>
      Option(entry.getProperty("groupId"))
    }.toSet

  def checkMultifactor(authenticatedUser: AuthenticatedUser) = {
    getGroupIds(authenticatedUser.user.email).contains(config.multifactorGroupId)
  }

}
