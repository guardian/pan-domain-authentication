import sbt._

object Dependencies {

  val awsDependencies = Seq("com.amazonaws" % "aws-java-sdk" % "1.7.5")

  val akkaDependencies = Seq("com.typesafe.akka" %% "akka-agent" % "2.3.4")

  val playLibs = Seq(
    "com.typesafe.play" %% "play" % "2.3.2" % "provided",
    "com.typesafe.play" %% "play-ws" % "2.3.2" % "provided",
    "commons-codec" % "commons-codec" % "1.9"
  )

  val gdataDependencies = Seq("com.google.gdata" % "core" % "1.47.1")

  val testDependencies = Seq("org.scalatest" %% "scalatest" % "2.1.5" % "test")

}
