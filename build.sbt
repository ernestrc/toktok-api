scalaVersion in ThisBuild := "2.11.6"

organization in ThisBuild := "Tokbox"

sbtVersion in ThisBuild := "0.13.7"

scalacOptions in ThisBuild ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-Ywarn-dead-code",
  "-language:_",
  "-target:jvm-1.7",
  "-encoding", "UTF-8"
)

resolvers in ThisBuild ++= Seq(
  "Typesafe Repository"       at        "http://repo.typesafe.com/typesafe/releases/",
  "Unstable Build Repo"       at        "http://dl.bintray.com/ernestrc/maven/",
  "Typesafe Repository Maven" at        "http://repo.typesafe.com/typesafe/maven-releases/",
  "Sonatype Snapshots"        at        "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases"         at        "http://oss.sonatype.org/content/repositories/releases",
  "Spray Repo"                at        "http://repo.spray.io",
  "Spray Nightlies"           at        "http://nightlies.spray.io"
)

testOptions in Test in ThisBuild += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console")

publishMavenStyle in ThisBuild:= true

publishArtifact in Test in ThisBuild := false

pomIncludeRepository in ThisBuild := { x => false }
