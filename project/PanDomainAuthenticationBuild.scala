import plugins.PlayArtifact._
import sbt._
import sbt.Keys._
import sbtassembly.Plugin.{AssemblyKeys, MergeStrategy}
import AssemblyKeys._
import Dependencies._
import sbtrelease._
import sbtrelease.ReleasePlugin._
import ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys
import xerial.sbt.Sonatype._
import com.typesafe.sbt.pgp.PgpKeys
import play.sbt.routes.RoutesKeys._


object PanDomainAuthenticationBuild extends Build {

  val scala211 = "2.11.8"

  val commonSettings =
    Seq(
      scalaVersion := scala211,
      scalaVersion in ThisBuild := scala211,
      crossScalaVersions := Seq(scala211),
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
      libraryDependencies ++= cryptoDependencies ++ testDependencies ++ httpClient ++ akkaDependencies ++ scheduler,
      publishArtifact := true
    )


  lazy val panDomainAuthCore = project("pan-domain-auth-core")
    .dependsOn(panDomainAuthVerification)
    .settings(sonatypeReleaseSettings: _*)
    .settings(
      libraryDependencies ++= akkaDependencies ++ awsDependencies ++ googleDirectoryApiDependencies ++ cryptoDependencies ++ testDependencies,
      publishArtifact := true
    )

  lazy val panDomainAuthPlay_2_4_0 = project("pan-domain-auth-play_2-4-0")
    .settings(sonatypeReleaseSettings: _*)
    .settings(
      libraryDependencies ++= playLibs_2_4_0,
      publishArtifact := true
    ).dependsOn(panDomainAuthCore)

  lazy val panDomainAuthPlay_2_5 = project("pan-domain-auth-play_2-5")
    .settings(sonatypeReleaseSettings: _*)
    .settings(
      libraryDependencies ++= playLibs_2_5,
      publishArtifact := true
    ).dependsOn(panDomainAuthCore)

  lazy val exampleApp = playProject("pan-domain-auth-example")
                  .settings(libraryDependencies ++= awsDependencies)
                  //.settings(playDefaultPort := 9500)
                  .dependsOn(panDomainAuthPlay_2_5)

  lazy val root = Project("pan-domain-auth-root", file(".")).aggregate(
    panDomainAuthVerification,
    panDomainAuthCore,
    panDomainAuthPlay_2_4_0,
    panDomainAuthPlay_2_5,
    exampleApp
  ).settings(sonatypeReleaseSettings: _*).settings(
      crossScalaVersions := Seq(scala211),
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
}
