import sbt._
import sbt.Keys._
import Dependencies._
import sbtrelease._
import sbtrelease.ReleasePlugin._
import ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys
import xerial.sbt.Sonatype._
import com.typesafe.sbt.pgp.PgpKeys

val scala212 = "2.12.8"

val commonSettings =
  Seq(
    scalaVersion := scala212,
    scalaVersion in ThisBuild := scala212,
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

lazy val panDomainAuthVerification = project("pan-domain-auth-verification")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies
      ++= cryptoDependencies
      ++ awsDependencies
      ++ testDependencies
      ++ jackson
      ++ loggingDependencies,
    publishArtifact := true
  )


lazy val panDomainAuthCore = project("pan-domain-auth-core")
  .dependsOn(panDomainAuthVerification)
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies ++= awsDependencies ++ googleDirectoryApiDependencies ++ cryptoDependencies ++ apacheHttpClient ++ testDependencies,
    publishArtifact := true
  )

lazy val panDomainAuthPlay_2_6 = project("pan-domain-auth-play_2-6")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies ++= playLibs_2_6,
    publishArtifact := true
  ).dependsOn(panDomainAuthCore)

lazy val panDomainAuthPlay_2_7 = project("pan-domain-auth-play_2-7")
  .settings(sonatypeReleaseSettings: _*)
  .settings(
    libraryDependencies ++= playLibs_2_7,
    publishArtifact := true
  ).dependsOn(panDomainAuthCore)

lazy val exampleApp = project("pan-domain-auth-example")
  .enablePlugins(PlayScala)
  .settings(libraryDependencies ++= (awsDependencies :+ ws))
  .dependsOn(panDomainAuthPlay_2_7)
  .settings(
    publishArtifact := false
  )

lazy val root = Project("pan-domain-auth-root", file(".")).aggregate(
  panDomainAuthVerification,
  panDomainAuthCore,
  panDomainAuthPlay_2_6,
  panDomainAuthPlay_2_7,
  exampleApp
).settings(sonatypeReleaseSettings: _*).settings(
  organization := "com.gu",
  publishArtifact := false
)

def project(path: String): Project =
  Project(path, file(path)).settings(commonSettings: _*)
