name := "gateway"

javaOptions in Revolver.reStart ++= Seq (
  "-Xmx2g"//,
  //"-javaagent:/etc/everreach/typesafe-console/lib/weaver/aspectjweaver.jar",
  //"-Dorg.aspectj.tracing.factory=default",
  //"-Djava.library.path=/etc/everreach/typesafe-console/lib/sigar"
)

Revolver.settings
