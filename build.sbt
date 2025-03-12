import sbt.*
import sbt.Keys.*
import Dependencies.*
import sbtrelease.*
import ReleaseStateTransformations.*
import play.sbt.PlayImport.PlayKeys.*
import sbtversionpolicy.withsbtrelease.ReleaseVersion

ThisBuild / scalaVersion := "3.3.5"
ThisBuild / crossScalaVersions := Seq(
  scalaVersion.value,
  "2.13.16"
)

val commonSettings = Seq(
  organization := "com.gu",
  licenses := Seq(License.Apache2),
  Test / fork := false,
  scalacOptions := Seq(
    "-feature",
    "-deprecation",
    "-release:11"
  ),
  Test / testOptions +=
    Tests.Argument(TestFrameworks.ScalaTest, "-u", s"test-results/scala-${scalaVersion.value}", "-o")
)

def subproject(path: String): Project =
  Project(path, file(path)).settings(commonSettings: _*)

lazy val panDomainAuthVerification = subproject("pan-domain-auth-verification")
  .settings(
    libraryDependencies
      ++= cryptoDependencies
      ++ awsDependencies
      ++ testDependencies
      ++ loggingDependencies,
  )

lazy val panDomainAuthCore = subproject("pan-domain-auth-core")
  .dependsOn(panDomainAuthVerification)
  .settings(
    libraryDependencies
      ++= awsDependencies
      ++ googleDirectoryApiDependencies
      ++ cryptoDependencies
      ++ testDependencies,
  )

def playBasedProject(playVersion: PlayVersion, projectPrefix: String, srcFolder: String) =
  subproject(s"$projectPrefix${playVersion.projectIdSuffix}").settings(
    sourceDirectory := (ThisBuild / baseDirectory).value / srcFolder / "src"
  )

def playSupportFor(playVersion: PlayVersion) =
  playBasedProject(playVersion, "pan-domain-auth", "pan-domain-auth-play").settings(
    libraryDependencies ++= playVersion.playLibs
  ).dependsOn(panDomainAuthCore)

def hmacPlayProject(playVersion: PlayVersion, playSupportProject: Project) =
  playBasedProject(playVersion, "panda-hmac", "pan-domain-auth-hmac").settings(
    libraryDependencies ++= hmacHeaders +: testDependencies
  ).dependsOn(playSupportProject)

lazy val panDomainAuthPlay_2_9 = playSupportFor(PlayVersion.V29)
lazy val panDomainAuthHmac_2_9 = hmacPlayProject(PlayVersion.V29, panDomainAuthPlay_2_9)

lazy val panDomainAuthPlay_3_0 = playSupportFor(PlayVersion.V30)
lazy val panDomainAuthHmac_3_0 = hmacPlayProject(PlayVersion.V30, panDomainAuthPlay_3_0)

lazy val exampleApp = subproject("pan-domain-auth-example")
  .enablePlugins(PlayScala)
  .dependsOn(panDomainAuthPlay_3_0)
  .settings(
    libraryDependencies ++= awsDependencies :+ ws,
    publish / skip := true,
    playDefaultPort := 9500
  )

lazy val root = Project("pan-domain-auth-root", file(".")).aggregate(
  panDomainAuthVerification,
  panDomainAuthCore,
  panDomainAuthPlay_2_9,
  panDomainAuthPlay_3_0,
  panDomainAuthHmac_2_9,
  panDomainAuthHmac_3_0,
  exampleApp
).settings(
  publish / skip := true,
  releaseVersion := ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease().value,
  releaseCrossBuild := true, // true if you cross-build the project for multiple Scala versions
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
