import sbt.Keys._
import sbt._

object TokTokBuild extends Build {

  val appName = "toktok"

  lazy val core = Project("core", file("core"))
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .dependsOn(krakken % "compile->compile;test->test")

  lazy val krakken = Project("krakken", file("krakken"))
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)

  lazy val users_command = Project("users_command", file("users_command"))
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .dependsOn(core % "compile->compile;test->test")

  lazy val users_query = Project("users_view", file("users_query"))
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)
    .dependsOn(core % "compile->compile;test->test")

  lazy val gateway = Project("gateway", file("gateway"))
    .settings(excludeFilter := FileFilter.globFilter("io.toktok.*.*.Boot"))
    .dependsOn(
      users_query % "compile->compile;test->test",
      users_command % "compile->compile;test->test")


  lazy val root = Project(appName, file("."))
    .aggregate(krakken, core, users_command, gateway, users_query)
}
