import sbt._

object Dependencies {

  val awsDependencies = Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.11.8")

  val akkaDependencies = Seq("com.typesafe.akka" %% "akka-agent" % "2.4.7")

  val scheduler = Seq("org.quartz-scheduler" % "quartz" % "2.2.3")

  val playLibs_2_4_0 = Seq(
    "com.typesafe.play" %% "play" % "2.4.0" % "provided",
    "com.typesafe.play" %% "play-ws" % "2.4.0" % "provided",
    "commons-codec" % "commons-codec" % "1.10",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.3"
  )

  val playLibs_2_5 = Seq(
    "com.typesafe.play" %% "play" % "2.5.4" % "provided",
    "com.typesafe.play" %% "play-ws" % "2.5.4" % "provided",
    "commons-codec" % "commons-codec" % "1.10"
  )

  val googleDirectoryApiDependencies = Seq(
    "com.google.api-client" % "google-api-client" % "1.22.0",
    "com.google.apis" % "google-api-services-admin" % "directory_v1-rev32-1.16.0-rc"
  )

  val cryptoDependencies = Seq(
    "org.bouncycastle" % "bcprov-jdk16" % "1.46",
    "commons-codec" % "commons-codec" % "1.10"
  )

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "2.2.6" % "test")

  val httpClient = Seq("net.databinder.dispatch" %% "dispatch-core" % "0.11.3")
}
