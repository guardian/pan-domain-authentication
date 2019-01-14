import PlayArtifact._
import sbt._
import sbt.Keys._
import sbtassembly.Plugin.{AssemblyKeys, MergeStrategy}
import AssemblyKeys._
import sbtrelease._
import sbtrelease.ReleasePlugin._
import ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys
import xerial.sbt.Sonatype._
import com.typesafe.sbt.pgp.PgpKeys
import play.sbt.routes.RoutesKeys._

val scala211 = "2.11.12"
val scala212 = "2.12.4"

val commonSettings =
  Seq(
    scalaVersion := scala212,
    scalaVersion in ThisBuild := scala212,
    crossScalaVersions := Seq(scala211, scala212),
    organization := "com.gu",
    fork in Test := false,
    resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"),
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
    publishArtifact := false
  )

val sonatypeReleaseSettings =
  releaseSettings ++ sonatypeSettings ++ Seq(
    licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/guardian/pan-domain-authentication"),
      "scm:git:git@github.com:guardian/pan-domain-authentication.git"
    )),
    pomExtra := {
      <url>https://github.com/guardian/an-domain-authentication</url>
        <developers>
          <developer>
            <id>steppenwells</id>
            <name>Stephen Wells</name>
            <url>https://github.com/steppenwells</url>
          </developer>
        </developers>
    },
    ReleaseKeys.crossBuild := true,
    ReleaseKeys.releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      ReleaseStep(
        action = { state =>
          val extracted = Project.extract(state)
          val ref = extracted.get(Keys.thisProjectRef)

          extracted.runAggregated(PgpKeys.publishSigned in Global in ref, state)
        },
        enableCrossBuild = true
      ),
      setNextVersion,
      commitNextVersion,
      ReleaseStep(
        action = state => Project.extract(state).runTask(SonatypeKeys.sonatypeReleaseAll, state)._1,
        enableCrossBuild = false
      ),
      pushChanges
    )
  )

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
  val version = "2.9.8"
  Seq(
    "com.fasterxml.jackson.core" % "jackson-core" % version,
    "com.fasterxml.jackson.core" % "jackson-databind" % version,
    "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % version
  )
}

// pin httpclient version to appease Snyk warning (https://app.snyk.io/vuln/SNYK-JAVA-ORGAPACHEHTTPCOMPONENTS-30646)
val apacheHttpClient = Seq("org.apache.httpcomponents" % "httpclient" % "4.5.5")

val logbackDependencies: Seq[ModuleID] = {
  val version = "1.2.3"
  Seq(
    "ch.qos.logback" % "logback-core" % version,
    "ch.qos.logback" % "logback-classic" % version
  )
}

lazy val panDomainAuthVerification = project("pan-domain-auth-verification")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies
      ++= cryptoDependencies
      ++ testDependencies
      ++ httpClient
      ++ akkaDependencies
      ++ scheduler
      ++ jackson
      ++ logbackDependencies,
    publishArtifact := true
  )


lazy val panDomainAuthCore = project("pan-domain-auth-core")
  .dependsOn(panDomainAuthVerification)
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies ++= akkaDependencies ++ awsDependencies ++ googleDirectoryApiDependencies ++ cryptoDependencies ++ apacheHttpClient ++ testDependencies,
    publishArtifact := true
  )

lazy val panDomainAuthPlay_2_6 = project("pan-domain-auth-play_2-6")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    crossScalaVersions := Seq(scala211, scala212),
    libraryDependencies ++= playLibs_2_6,
    publishArtifact := true
  ).dependsOn(panDomainAuthCore)

lazy val exampleApp = playProject("pan-domain-auth-example")
  .settings(libraryDependencies ++= awsDependencies)
  //.settings(playDefaultPort := 9500)
  .dependsOn(panDomainAuthPlay_2_6)

lazy val root = Project("pan-domain-auth-root", file(".")).aggregate(
  panDomainAuthVerification,
  panDomainAuthCore,
  panDomainAuthPlay_2_6,
  exampleApp
).settings(sonatypeReleaseSettings: _*).settings(
  crossScalaVersions := Seq(scala211, scala212),
  organization := "com.gu",
  publishArtifact := false
)

def project(path: String): Project =
  Project(path, file(path)).settings(commonSettings: _*)

def playProject(path: String): Project =
  Project(path, file(path)).enablePlugins(play.sbt.PlayScala)
    .settings(commonSettings ++ sonatypeReleaseSettings ++ playArtifactDistSettings ++ playArtifactSettings: _*)
    .settings(
      libraryDependencies += play.sbt.PlayImport.ws,
      routesGenerator := InjectedRoutesGenerator
    )
    .settings(magentaPackageName := path)

def playArtifactSettings = Seq(
  ivyXML :=
    <dependencies>
      <exclude org="commons-logging"/>
      <exclude org="org.springframework"/>
      <exclude org="org.scala-tools.sbt"/>
    </dependencies>,
  mergeStrategy in assembly <<= (mergeStrategy in assembly) { old => {
    case f if f.startsWith("org/apache/lucene/index/") => MergeStrategy.first
    case "play/core/server/ServerWithStop.class" => MergeStrategy.first
    case "ehcache.xml" => MergeStrategy.first
    case x => old(x)
  }}
)
