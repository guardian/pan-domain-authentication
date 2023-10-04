import sbt._

object Dependencies {

  val awsDependencies = Seq("com.amazonaws" % "aws-java-sdk-s3" % "1.12.470")

  val playLibs_2_8 = {
    val version = "2.8.19"
    Seq(
      "com.typesafe.play" %% "play" % version % "provided",
      "com.typesafe.play" %% "play-ws" % version % "provided"
    )
  }

  val googleDirectoryApiDependencies = Seq(
    "com.google.apis" % "google-api-services-admin-directory" % "directory_v1-rev20230124-2.0.0",
    "com.google.auth" % "google-auth-library-credentials" % "1.16.0",
    "com.google.auth" % "google-auth-library-oauth2-http" % "1.16.0",
    /*
     * Normally transitive from the above, pull up manually to fix:
        - https://app.snyk.io/vuln/SNYK-JAVA-COMGOOGLEOAUTHCLIENT-575276
        - https://security.snyk.io/vuln/SNYK-JAVA-COMGOOGLEOAUTHCLIENT-2807808
     */
    "com.google.oauth-client" % "google-oauth-client" % "1.33.3"
  )

  val cryptoDependencies = Seq(
    "org.bouncycastle" % "bcprov-jdk15on" % "1.69",
    "commons-codec" % "commons-codec" % "1.14"
  )

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "3.2.0" % "test")

  val loggingDependencies = Seq("org.slf4j" % "slf4j-api" % "1.7.30")

  // provide compatibility between scala 2.12 and 2.13
  // see https://github.com/scala/scala-collection-compat/issues/208
  val scalaCollectionCompatDependencies = Seq("org.scala-lang.modules" %% "scala-collection-compat" % "2.1.6")
}
