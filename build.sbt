import sbt.*
import sbt.Keys.*
import Dependencies.*
import sbtrelease.*
import ReleaseStateTransformations.*
import play.sbt.PlayImport.PlayKeys.*
import sbtversionpolicy.withsbtrelease.ReleaseVersion

ThisBuild / scalaVersion := "3.3.7"
val crossCompileScalaVersions = crossScalaVersions := Seq(scalaVersion.value, "2.13.16")

val commonSettings = Seq(
  Test / fork := false,
  libraryDependencies ++= testDependencies,
  Test / testOptions +=
    Tests.Argument(TestFrameworks.ScalaTest, "-u", s"test-results/scala-${scalaVersion.value}", "-o")
)

val artifactProductionSettings = Seq(
  crossCompileScalaVersions, // mostly, we only want to cross-compile Scala version for projects that create artifacts
  organization := "com.gu",
  licenses := Seq(License.Apache2),
  scalacOptions := Seq(
    "-feature",
    "-deprecation",
    "-release:11"
  ),
)

def directSubfolderProject(path: String): Project = Project(path, file(path)).settings(commonSettings)

lazy val panDomainAuthVerification = directSubfolderProject("pan-domain-auth-verification")
  .settings(
    artifactProductionSettings,
    libraryDependencies ++= cryptoDependencies ++ awsDependencies ++ loggingDependencies ++ jacksonDependencies
  )

lazy val panDomainAuthCore = directSubfolderProject("pan-domain-auth-core")
  .dependsOn(panDomainAuthVerification)
  .settings(
    artifactProductionSettings,
    libraryDependencies ++= googleDirectoryApiDependencies
  )

def playSupportFor(playVersion: PlayVersion) = directSubfolderProject(s"pan-domain-auth-${playVersion.suffix}")
  .dependsOn(panDomainAuthCore)
  .settings(
    artifactProductionSettings,
    sourceDirectory := (ThisBuild / baseDirectory).value / "pan-domain-auth-play" / "src",
    libraryDependencies ++= playVersion.playLibs
  )

def hmacProject(nameSuffix: String, subFolderPath: String) =
  Project(s"panda-hmac-$nameSuffix", file(s"hmac/$subFolderPath")).settings(
    commonSettings,
    artifactProductionSettings // all HMAC projects are published
  )

def hmacPlayProject(playVersion: PlayVersion, playSupportProject: Project) =
  hmacProject(playVersion.suffix, s"play/${playVersion.suffix}")
    .dependsOn(playSupportProject, panDomainAuthHmac)

lazy val panDomainAuthHmac = hmacProject("core", "core").settings(libraryDependencies += hmacHeaders)

lazy val panDomainAuthPlay_2_9 = playSupportFor(PlayVersion.V29)
lazy val panDomainAuthHmac_2_9 = hmacPlayProject(PlayVersion.V29, panDomainAuthPlay_2_9)

lazy val panDomainAuthPlay_3_0 = playSupportFor(PlayVersion.V30)
lazy val panDomainAuthHmac_3_0 = hmacPlayProject(PlayVersion.V30, panDomainAuthPlay_3_0)

lazy val exampleApp = directSubfolderProject("pan-domain-auth-example")
  .enablePlugins(PlayScala)
  .dependsOn(panDomainAuthPlay_3_0)
  .settings(
    crossCompileScalaVersions, // IntelliJ seems to require this to successfully import the sbt project
    libraryDependencies ++= awsDependencies :+ ws,
    publish / skip := true,
    playDefaultPort := 9500
  )

lazy val keyRotation = directSubfolderProject("key-rotation")
  .dependsOn(panDomainAuthVerification)
  .settings(
    publish / skip := true
  )

lazy val root = Project("pan-domain-auth-root", file(".")).aggregate(
  panDomainAuthVerification,
  panDomainAuthCore,
  panDomainAuthPlay_2_9,
  panDomainAuthPlay_3_0,
  panDomainAuthHmac,
  panDomainAuthHmac_2_9,
  panDomainAuthHmac_3_0,
  exampleApp,
  keyRotation
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
