import sbt._

object Dependencies {

  val awsDependencies = Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.11.835")

  val playLibs_2_6 = {
    val version = "2.6.25"
    Seq(
      "com.typesafe.play" %% "play" % version % "provided",
      "com.typesafe.play" %% "play-ws" % version % "provided"
    )
  }

  val playLibs_2_7 = {
    val version = "2.7.5"
    Seq(
      "com.typesafe.play" %% "play" % version % "provided",
      "com.typesafe.play" %% "play-ws" % version % "provided"
    )
  }

  val playLibs_2_8 = {
    val version = "2.8.2"
    Seq(
      "com.typesafe.play" %% "play" % version % "provided",
      "com.typesafe.play" %% "play-ws" % version % "provided"
    )
  }

  val googleDirectoryApiDependencies = Seq(
    "com.google.auth" % "google-auth-library-oauth2-http" % "0.21.1",
    "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev118-1.25.0"
  )

  val cryptoDependencies = Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % "1.66",
    "commons-codec" % "commons-codec" % "1.14"
  )

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "3.2.0" % "test")

  /*
  * Pull in an updated version of jackson and logback libraries as the ones AWS use have security vulnerabilities.
  * See https://github.com/aws/aws-sdk-java/pull/1373
  * */
  val jackson: Seq[ModuleID] = {
    val version = "2.11.2"
    Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % version,
      "com.fasterxml.jackson.core" % "jackson-databind" % version,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % version
    )
  }

  val loggingDependencies = Seq("org.slf4j" % "slf4j-api" % "1.7.30")
}
