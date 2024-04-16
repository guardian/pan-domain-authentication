import sbt._
import sbt.Keys._
import Dependencies._
import sbtrelease._
import ReleaseStateTransformations._
import xerial.sbt.Sonatype._
import play.sbt.PlayImport.PlayKeys._
import sbtversionpolicy.withsbtrelease.ReleaseVersion

val scala212 = "2.12.15"
val scala213 = "2.13.8"

ThisBuild / scalaVersion := scala213

val commonSettings =
  Seq(
    crossScalaVersions := List(scala212, scala213),
    organization := "com.gu",
    Test / fork := false,
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      // upgrade warnings to errors except deprecations
      "-Wconf:cat=deprecation:ws,any:e"
    ),
    publishArtifact := false
  )

val sonatypeReleaseSettings = {
  sonatypeSettings ++ Seq(
    licenses := Seq(License.Apache2),
    // sbt and sbt-release implement cross-building support differently. sbt does it better
    // (it supports each subproject having different crossScalaVersions), so disable sbt-release's
    // implementation, and do the publish step with a `+`,
    // ie. (`releaseStepCommandAndRemaining("+publishSigned")`)
    // See https://www.scala-sbt.org/1.x/docs/Cross-Build.html#Note+about+sbt-release
    // Never run with "release cross" or "+release"! Odd things start happening
    releaseCrossBuild := false,
    releaseVersion := ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease().value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion
    )
  )
}

lazy val panDomainAuthVerification = project("pan-domain-auth-verification")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies
      ++= cryptoDependencies
      ++ awsDependencies
      ++ testDependencies
      ++ loggingDependencies
      ++ scalaCollectionCompatDependencies,
  )


lazy val panDomainAuthCore = project("pan-domain-auth-core")
  .dependsOn(panDomainAuthVerification)
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies
      ++= awsDependencies
      ++ googleDirectoryApiDependencies
      ++ cryptoDependencies
      ++ testDependencies
      ++ scalaCollectionCompatDependencies,
  )

lazy val panDomainAuthPlay_2_8 = project("pan-domain-auth-play_2-8")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies
      ++= playLibs_2_8
      ++ scalaCollectionCompatDependencies,
  ).dependsOn(panDomainAuthCore)

lazy val panDomainAuthPlay_2_9 = project("pan-domain-auth-play_2-9")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies
      ++= playLibs_2_9
      ++ scalaCollectionCompatDependencies,
  ).dependsOn(panDomainAuthCore)

lazy val panDomainAuthPlay_3_0 = project("pan-domain-auth-play_3-0")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies
      ++= playLibs_3_0
      ++ scalaCollectionCompatDependencies,
  ).dependsOn(panDomainAuthCore)

lazy val panDomainAuthHmac_2_8 = project("panda-hmac-play_2-8")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-hmac" / "src")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies ++= hmacLibs ++ playLibs_2_8 ++ testDependencies,
  ).dependsOn(panDomainAuthPlay_2_8)

lazy val panDomainAuthHmac_2_9 = project("panda-hmac-play_2-9")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-hmac" / "src")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies ++= hmacLibs ++ playLibs_2_9 ++ testDependencies,
  ).dependsOn(panDomainAuthPlay_2_9)

lazy val panDomainAuthHmac_3_0 = project("panda-hmac-play_3-0")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-hmac" / "src")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies ++= hmacLibs ++ playLibs_3_0 ++ testDependencies,
  ).dependsOn(panDomainAuthPlay_3_0)

lazy val exampleApp = project("pan-domain-auth-example")
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= (awsDependencies :+ ws))
  .dependsOn(panDomainAuthPlay_2_9)
  .settings(
    crossScalaVersions := Seq(scala213),
    publish / skip := true,
    playDefaultPort := 9500
  )

lazy val root = Project("pan-domain-auth-root", file(".")).aggregate(
  panDomainAuthVerification,
  panDomainAuthCore,
  panDomainAuthPlay_2_8,
  panDomainAuthPlay_2_9,
  panDomainAuthPlay_3_0,
  panDomainAuthHmac_2_8,
  panDomainAuthHmac_2_9,
  panDomainAuthHmac_3_0,
  exampleApp
).settings(sonatypeReleaseSettings)
 .settings(
  organization := "com.gu",
  publish / skip := true,
)

def project(path: String): Project =
  Project(path, file(path)).settings(commonSettings: _*)
