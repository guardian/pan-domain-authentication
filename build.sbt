import sbt._
import sbt.Keys._
import Dependencies._
import sbtrelease._
import ReleaseStateTransformations._
import xerial.sbt.Sonatype._
import play.sbt.PlayImport.PlayKeys._

val scala212 = "2.12.15"
val scala213 = "2.13.8"

ThisBuild / scalaVersion := scala212

val commonSettings =
  Seq(
    scalaVersion := scala212,
    crossScalaVersions := Seq(scalaVersion.value, scala213),
    organization := "com.gu",
    Test / fork := false,
    scalacOptions ++= Seq("-feature", "-deprecation", "-Xfatal-warnings"),
    publishArtifact := false
  )

val sonatypeReleaseSettings = {
  sonatypeSettings ++ Seq(
    publishTo := sonatypePublishToBundle.value,
    licenses := Seq("Apache V2" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/guardian/pan-domain-authentication"),
      "scm:git:git@github.com:guardian/pan-domain-authentication.git"
    )),
    developers := List(Developer(
      id = "GuardianEdTools",
      name = "Guardian Editorial Tools",
      email = "digitalcms.dev@theguardian.com",
      url = url("https://github.com/orgs/guardian/teams/digital-cms")
    )),
    releaseCrossBuild := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      // For non cross-build projects, use releaseStepCommand("publishSigned")
      releaseStepCommandAndRemaining("+publishSigned"),
      releaseStepCommand("sonatypeBundleRelease"),
      setNextVersion,
      commitNextVersion,
      pushChanges
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
    publishArtifact := true
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
    publishArtifact := true
  )

lazy val panDomainAuthPlay_2_8 = project("pan-domain-auth-play_2-8")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies
      ++= playLibs_2_8
      ++ scalaCollectionCompatDependencies,
    publishArtifact := true
  ).dependsOn(panDomainAuthCore)

lazy val exampleApp = project("pan-domain-auth-example")
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= (awsDependencies :+ ws))
  .dependsOn(panDomainAuthPlay_2_8)
  .settings(
    publishArtifact := false,
    publish / skip := true,
    playDefaultPort := 9500
  )

lazy val root = Project("pan-domain-auth-root", file(".")).aggregate(
  panDomainAuthVerification,
  panDomainAuthCore,
  panDomainAuthPlay_2_8,
  exampleApp
).settings(
  organization := "com.gu",
  publishArtifact := false,
  publish / skip := true,
)

def project(path: String): Project =
  Project(path, file(path)).settings(commonSettings: _*)
