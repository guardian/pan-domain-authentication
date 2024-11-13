import sbt._

object Dependencies {

  val awsDependencies = Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.12.772")

  val playLibs_2_8 = {
    val version = "2.8.19"
    Seq(
      "com.typesafe.play" %% "play" % version % "provided",
      "com.typesafe.play" %% "play-ws" % version % "provided"
    )
  }

  val playLibs_2_9 = {
    val version = "2.9.0"
    Seq(
      "com.typesafe.play" %% "play" % version % "provided",
      "com.typesafe.play" %% "play-ws" % version % "provided"
    )
  }

  val playLibs_3_0 = {
    val version = "3.0.0"
    Seq(
      "org.playframework" %% "play" % version % "provided",
      "org.playframework" %% "play-ws" % version % "provided"
    )
  }

  val hmacLibs = Seq(
    "com.gu" %% "hmac-headers" % "2.0.0"
  )

  val googleDirectoryApiDependencies = Seq(
    "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev20241029-2.0.0",
    "com.google.auth" % "google-auth-library-credentials" % "1.30.0",
    "com.google.auth" % "google-auth-library-oauth2-http" % "1.30.0",
  )

  val cryptoDependencies = Seq(
    "org.bouncycastle" % "bcprov-jdk18on" % "1.78.1",
    "commons-codec" % "commons-codec" % "1.17.1"
  )

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "3.2.19" % Test)

  val loggingDependencies = Seq("org.slf4j" % "slf4j-api" % "1.7.36")

  // provide compatibility between scala 2.12 and 2.13
  // see https://github.com/scala/scala-collection-compat/issues/208
  val scalaCollectionCompatDependencies = Seq("org.scala-lang.modules" %% "scala-collection-compat" % "2.12.0")
}
