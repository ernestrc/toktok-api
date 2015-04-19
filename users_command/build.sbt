name := "users_command"

libraryDependencies ++= List(
  "com.tokbox"               % "opentok-server-sdk"         % "2.2.2" exclude("org.jboss.netty","netty")
  )

javaOptions in Revolver.reStart ++= Seq (
  "-Xmx2g"//,
  //"-javaagent:/etc/everreach/typesafe-console/lib/weaver/aspectjweaver.jar",
  //"-Dorg.aspectj.tracing.factory=default",
  //"-Djava.library.path=/etc/everreach/typesafe-console/lib/sigar"
)

Revolver.settings
