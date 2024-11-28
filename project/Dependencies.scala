import sbt._

object Dependencies {

  val awsDependencies = Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.12.772")

  case class PlayVersion(
    majorVersion: Int,
    minorVersion: Int,
    groupId: String,
    exactPlayVersion: String
  ) {
    val projectIdSuffix = s"-play_$majorVersion-$minorVersion"

    val playLibs: Seq[ModuleID] =
      Seq("play", "play-ws").map(artifact => groupId %% artifact % exactPlayVersion)
  }

  object PlayVersion {
    val V29 = PlayVersion(2, 9, "com.typesafe.play", "2.9.2")
    val V30 = PlayVersion(3, 0, "org.playframework", "3.0.5")
  }

  val hmacHeaders = "com.gu" %% "hmac-headers" % "2.0.1"

  val googleDirectoryApiDependencies = Seq(
    "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev20240903-2.0.0",
    "com.google.auth" % "google-auth-library-credentials" % "1.16.1",
    "com.google.auth" % "google-auth-library-oauth2-http" % "1.16.1",
  )

  val cryptoDependencies = Seq(
    "org.bouncycastle" % "bcprov-jdk18on" % "1.78.1",
    "commons-codec" % "commons-codec" % "1.17.1"
  )

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "3.2.19" % Test)

  val loggingDependencies = Seq("org.slf4j" % "slf4j-api" % "1.7.36")

  // provide compatibility between scala 2.12 and 2.13
  // see https://github.com/scala/scala-collection-compat/issues/208
  val scalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.12.0"
}
