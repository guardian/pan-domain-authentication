import sbt._

object Dependencies {

  val awsDependencies = Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.10.45")

  val akkaDependencies = Seq("com.typesafe.akka" %% "akka-agent" % "2.3.4")

  val scheduler = Seq("org.quartz-scheduler" % "quartz" % "2.2.1")

  val playLibs = Seq(
    "com.typesafe.play" %% "play" % "2.3.2" % "provided",
    "com.typesafe.play" %% "play-ws" % "2.3.2" % "provided",
    "commons-codec" % "commons-codec" % "1.9"
  )

  val playLibs_2_4_0 = Seq(
    "com.typesafe.play" %% "play" % "2.4.0" % "provided",
    "com.typesafe.play" %% "play-ws" % "2.4.0" % "provided",
    "commons-codec" % "commons-codec" % "1.9"
  )

  val googleDirectoryApiDependencies = Seq(
    "com.google.api-client" % "google-api-client" % "1.19.1",
    "com.google.apis" % "google-api-services-admin" % "directory_v1-rev32-1.16.0-rc"
  )

  val cryptoDependencies = Seq(
    "org.bouncycastle" % "bcprov-jdk16" % "1.46",
    "commons-codec" % "commons-codec" % "1.9"
  )

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "2.1.5" % "test")

  val httpClient = Seq("net.databinder.dispatch" %% "dispatch-core" % "0.11.2")
}
