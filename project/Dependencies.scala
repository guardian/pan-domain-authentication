import sbt._

object Dependencies {

  val awsDependencies = Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.11.461")

  val akkaDependencies = Seq("com.typesafe.akka" %% "akka-agent" % "2.4.20")

  val scheduler = Seq("org.quartz-scheduler" % "quartz" % "2.2.3")

  val playLibs_2_6 = {
    val version = "2.6.11"
    Seq(
      "com.typesafe.play" %% "play" % version % "provided",
      "com.typesafe.play" %% "play-ws" % version % "provided",
      "commons-codec" % "commons-codec" % "1.10",
      "joda-time" % "joda-time" % "2.9.9",
      "org.joda" % "joda-convert" % "1.9.2"

    )
  }

  val googleDirectoryApiDependencies = Seq(
    "com.google.api-client" % "google-api-client" % "1.22.0",
    "com.google.apis" % "google-api-services-admin" % "directory_v1-rev32-1.16.0-rc"
  )

  val cryptoDependencies = Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % "1.60",
    "commons-codec" % "commons-codec" % "1.10"
  )

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "3.0.4" % "test")

  val httpClient = Seq("com.squareup.okhttp3" % "okhttp" % "3.9.1")

  /*
  * Pull in an updated version of jackson and logback libraries as the ones AWS use have security vulnerabilities.
  * See https://github.com/aws/aws-sdk-java/pull/1373
  * */
  val jackson: Seq[ModuleID] = {
    val version = "2.9.2"
    Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % version,
      "com.fasterxml.jackson.core" % "jackson-databind" % version,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % version
    )
  }

  // pin httpclient version to appease Snyk warning (https://app.snyk.io/vuln/SNYK-JAVA-ORGAPACHEHTTPCOMPONENTS-30646)
  val apacheHttpClient = Seq("org.apache.httpcomponents" % "httpclient" % "4.5.5")

  val logback: Seq[ModuleID] = {
    val version = "1.2.3"
    Seq(
      "ch.qos.logback" % "logback-core" % version,
      "ch.qos.logback" % "logback-classic" % version
    )
  }
}
