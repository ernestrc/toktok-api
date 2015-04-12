name := "core"

parallelExecution in Test := false

testOptions in Test += Tests.Argument(TestFrameworks.Specs2, "junitxml", "console")
