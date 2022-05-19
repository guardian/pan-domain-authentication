import sbt._
import sbt.Keys._
import Dependencies._
import sbtrelease._
import ReleaseStateTransformations._
import xerial.sbt.Sonatype._
import play.sbt.PlayImport.PlayKeys._

val scala212 = "2.12.15"
val scala213 = "2.13.8"

val commonSettings =
  Seq(
    scalaVersion := scala212,
    scalaVersion in ThisBuild := scala212,
    crossScalaVersions := Seq(scalaVersion.value, scala213),
    organization := "com.gu",
    fork in Test := false,
    resolvers ++= Seq("Typesafe Repository" at "https://repo.typesafe.com/typesafe/releases/"),
    scalacOptions ++= Seq("-feature", "-deprecation", "-language:higherKinds", "-Xfatal-warnings"),
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
    pomExtra := {
      <url>https://github.com/guardian/pan-domain-authentication</url>
        <developers>
          <developer>
            <id>Guardian Developers</id>
            <name>Guardian Developers</name>
            <url>https://github.com/guardian</url>
          </developer>
        </developers>
    },
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
      ++ jackson
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

lazy val panDomainAuthPlay_2_7 = project("pan-domain-auth-play_2-7")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies
      ++= playLibs_2_7
      ++ scalaCollectionCompatDependencies,
    publishArtifact := true
  ).dependsOn(panDomainAuthCore)

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
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    publishArtifact := false,
    skip in publish := true,
    playDefaultPort := 9500
  )

lazy val root = Project("pan-domain-auth-root", file(".")).aggregate(
  panDomainAuthVerification,
  panDomainAuthCore,
  panDomainAuthPlay_2_7,
  panDomainAuthPlay_2_8,
  exampleApp
).settings(sonatypeReleaseSettings: _*).settings(
  organization := "com.gu",
  publishArtifact := false,
  skip in publish := true,
)

def project(path: String): Project =
  Project(path, file(path)).settings(commonSettings: _*)
