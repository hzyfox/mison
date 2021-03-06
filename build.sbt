lazy val commonSettings = Seq(
  scalaVersion := "2.12.3"
)

lazy val native =
  (project in file("native")).
  settings(
    compile := {
      import scala.sys.process._
      Process("cargo build --release", file("native"), "RUSTFLAGS" -> "-C target-cpu=native").!
      (compile in Compile).value
    }
  )

lazy val macros =
  (project in file("macros")).
  settings(
    commonSettings,
    libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
  )

lazy val mison =
  (project in file("mison")).
  settings(
    commonSettings,
    fork in run := true,
    javaOptions in run := Seq(s"-Duser.dir=${file(".").getAbsolutePath}")
  ).
  dependsOn(native % "compile-internal", macros % "compile-internal")

lazy val benchmarks =
  (project in file("benchmarks")).
  settings(
    commonSettings,
    javaOptions in Jmh := Seq("-Xcomp", s"-Duser.dir=${file(".").getAbsolutePath}"),
    libraryDependencies += "com.alibaba" % "fastjson" % "1.2.38",
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1"
  ).
  enablePlugins(JmhPlugin).
  dependsOn(mison)

lazy val root =
  (project in file(".")).
  aggregate(macros, native, mison, benchmarks)