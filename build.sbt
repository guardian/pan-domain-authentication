import sbt._
import sbt.Keys._
import Dependencies._
import sbtrelease._
import ReleaseStateTransformations._
import xerial.sbt.Sonatype._
import play.sbt.PlayImport.PlayKeys._

val scala212 = "2.12.15"
val scala213 = "2.13.8"

ThisBuild / scalaVersion := scala213

// See below - the release process itself is correctly configured to publish the cross-built
// subprojects, invoking sbt with + or sbt-release with "release cross" only serves to confuse things.
// Always run release as `sbt clean release`!
val checkRunCorrectly = ReleaseStep(action = st => {
  val allcommands = (st.history.executed ++ st.currentCommand ++ st.remainingCommands).map(_.commandLine)
  val crossCommands = allcommands.exists(_ contains "+")
  val releaseCommandWithArgs = allcommands.exists(cmd => cmd.contains("release") && cmd != "release")
  
  if (crossCommands) {
    st.log.error("Don't run release commands with cross building! Try again with 'sbt clean release'.")
    sys.exit(1)
  } else if (releaseCommandWithArgs) {
    st.log.error("Don't run the release command with arguments! Try again with 'sbt clean release'.")
    sys.exit(1)
  }

  st
})

val commonSettings =
  Seq(
    crossScalaVersions := List(scala212, scala213),
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
    homepage := Some(url("https://github.com/guardian/pan-domain-authentication")),
    developers := List(Developer(
      id = "GuardianEdTools",
      name = "Guardian Editorial Tools",
      email = "digitalcms.dev@theguardian.com",
      url = url("https://github.com/orgs/guardian/teams/digital-cms")
    )),
    // sbt and sbt-release implement cross-building support differently. sbt does it better
    // (it supports each subproject having different crossScalaVersions), so disable sbt-release's
    // implementation, and do the publish step with a `+`,
    // ie. (`releaseStepCommandAndRemaining("+publishSigned")`)
    // See https://www.scala-sbt.org/1.x/docs/Cross-Build.html#Note+about+sbt-release
    // Never run with "release cross" or "+release"! Odd things start happening
    releaseCrossBuild := false,
    releaseProcess := Seq[ReleaseStep](
      checkRunCorrectly,
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
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies
      ++= playLibs_2_8
      ++ scalaCollectionCompatDependencies,
    publishArtifact := true
  ).dependsOn(panDomainAuthCore)

lazy val panDomainAuthPlay_2_9 = project("pan-domain-auth-play_2-9")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies
      ++= playLibs_2_9
      ++ scalaCollectionCompatDependencies,
    publishArtifact := true
  ).dependsOn(panDomainAuthCore)

lazy val panDomainAuthPlay_3_0 = project("pan-domain-auth-play_3-0")
  .settings(sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    crossScalaVersions := Seq(scala213),
    libraryDependencies
      ++= playLibs_3_0
      ++ scalaCollectionCompatDependencies,
    publishArtifact := true
  ).dependsOn(panDomainAuthCore)

lazy val exampleApp = project("pan-domain-auth-example")
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= (awsDependencies :+ ws))
  .dependsOn(panDomainAuthPlay_2_9)
  .settings(
    crossScalaVersions := Seq(scala213),
    publishArtifact := false,
    publish / skip := true,
    playDefaultPort := 9500
  )

lazy val root = Project("pan-domain-auth-root", file(".")).aggregate(
  panDomainAuthVerification,
  panDomainAuthCore,
  panDomainAuthPlay_2_8,
  panDomainAuthPlay_2_9,
  panDomainAuthPlay_3_0,
  exampleApp
).settings(sonatypeReleaseSettings)
 .settings(
  organization := "com.gu",
  publishArtifact := false,
  publish / skip := true,
)

def project(path: String): Project =
  Project(path, file(path)).settings(commonSettings: _*)
