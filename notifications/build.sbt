name := "notifications"

libraryDependencies ++= List(
  "com.postmark"               %% "postmark-spray"         % "0.4.2")

javaOptions in Revolver.reStart ++= Seq (
  "-Xmx2g"//,
  //"-javaagent:/etc/everreach/typesafe-console/lib/weaver/aspectjweaver.jar",
  //"-Dorg.aspectj.tracing.factory=default",
  //"-Djava.library.path=/etc/everreach/typesafe-console/lib/sigar"
)

Revolver.settings