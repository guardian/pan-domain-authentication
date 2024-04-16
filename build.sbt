import sbt._
import sbt.Keys._
import Dependencies._
import sbtrelease._
import ReleaseStateTransformations._
import xerial.sbt.Sonatype._
import play.sbt.PlayImport.PlayKeys._
import sbtversionpolicy.withsbtrelease.ReleaseVersion

val scala212 = "2.12.17"
val scala213 = "2.13.9"

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
      "-Wconf:cat=deprecation:ws,any:e",
      "-release:8"
    ),
    licenses := Seq(License.Apache2),
  )

lazy val panDomainAuthVerification = subproject("pan-domain-auth-verification")
  .settings(
    libraryDependencies
      ++= cryptoDependencies
      ++ awsDependencies
      ++ testDependencies
      ++ loggingDependencies
      ++ scalaCollectionCompatDependencies,
  )


lazy val panDomainAuthCore = subproject("pan-domain-auth-core")
  .dependsOn(panDomainAuthVerification)
  .settings(
    libraryDependencies
      ++= awsDependencies
      ++ googleDirectoryApiDependencies
      ++ cryptoDependencies
      ++ testDependencies
      ++ scalaCollectionCompatDependencies,
  )

lazy val panDomainAuthPlay_2_8 = subproject("pan-domain-auth-play_2-8")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src")
  .settings(
    libraryDependencies
      ++= playLibs_2_8
      ++ scalaCollectionCompatDependencies,
  ).dependsOn(panDomainAuthCore)

lazy val panDomainAuthPlay_2_9 = subproject("pan-domain-auth-play_2-9")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src")
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies
      ++= playLibs_2_9
      ++ scalaCollectionCompatDependencies,
  ).dependsOn(panDomainAuthCore)

lazy val panDomainAuthPlay_3_0 = subproject("pan-domain-auth-play_3-0")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src")
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies
      ++= playLibs_3_0
      ++ scalaCollectionCompatDependencies,
  ).dependsOn(panDomainAuthCore)

lazy val panDomainAuthHmac_2_8 = subproject("panda-hmac-play_2-8")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-hmac" / "src")
  .settings(
    libraryDependencies ++= hmacLibs ++ playLibs_2_8 ++ testDependencies,
  ).dependsOn(panDomainAuthPlay_2_8)

lazy val panDomainAuthHmac_2_9 = subproject("panda-hmac-play_2-9")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-hmac" / "src")
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies ++= hmacLibs ++ playLibs_2_9 ++ testDependencies,
  ).dependsOn(panDomainAuthPlay_2_9)

lazy val panDomainAuthHmac_3_0 = subproject("panda-hmac-play_3-0")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-hmac" / "src")
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies ++= hmacLibs ++ playLibs_3_0 ++ testDependencies,
  ).dependsOn(panDomainAuthPlay_3_0)

lazy val exampleApp = subproject("pan-domain-auth-example")
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= (awsDependencies :+ ws))
  .dependsOn(panDomainAuthPlay_2_9)
  .settings(
    crossScalaVersions := Seq(scala213),
    publish / skip := true,
    playDefaultPort := 9500
  )

lazy val sonatypeReleaseSettings = {
  sonatypeSettings ++ Seq(
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

def subproject(path: String): Project =
  Project(path, file(path)).settings(commonSettings: _*)
