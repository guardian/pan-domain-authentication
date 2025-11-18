import sbt._

object Dependencies {

  val awsDependencies = Seq("software.amazon.awssdk" % "s3" % "2.33.6")

  case class PlayVersion(
    majorVersion: Int,
    minorVersion: Int,
    groupId: String,
    exactPlayVersion: String
  ) {
    val suffix = s"play_$majorVersion-$minorVersion"

    val playLibs: Seq[ModuleID] =
      Seq("play", "play-ws").map(artifact => groupId %% artifact % exactPlayVersion)
  }

  object PlayVersion {
    val V29 = PlayVersion(2, 9, "com.typesafe.play", "2.9.9")
    val V30 = PlayVersion(3, 0, "org.playframework", "3.0.9")
  }

  val hmacHeaders = "com.gu" %% "hmac-headers" % "2.0.1"

  val googleDirectoryApiDependencies = Seq(
    "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev20240903-2.0.0",
    "com.google.auth" % "google-auth-library-credentials" % "1.40.0",
    "com.google.auth" % "google-auth-library-oauth2-http" % "1.40.0",
  )

  val cryptoDependencies = Seq(
    "org.bouncycastle" % "bcprov-jdk18on" % "1.78.1",
    "commons-codec" % "commons-codec" % "1.17.1",
    "com.google.guava" % "guava" % "33.4.0-jre"
  )

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "3.2.19" % Test)

  val loggingDependencies = Seq("org.slf4j" % "slf4j-api" % "1.7.36")

  val jacksonVersion = "2.20.0"

  // A transient dependency added to evict vulnerable Jackson versions
  val jacksonDependencies = Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
    "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
    "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % jacksonVersion,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion
  )

}
