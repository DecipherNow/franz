import sbt.Keys.javaOptions

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    organization := "com.deciphernow",
    name         := "franz",
    version      := "1.0.0-SNAPSHOT",

    scalaVersion := "2.11.11",

    libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "0.14",
    libraryDependencies += "com.deciphernow" % "moby-dns" % "1.0.0" % "it",
    libraryDependencies += "com.whisk" %% "docker-testkit-scalatest" % "0.9.3" % "it",
    libraryDependencies += "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.3" % "it",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test, it",

    fork in IntegrationTest := true,
    javaOptions in IntegrationTest += "-Dsun.net.spi.nameservice.provider.1=dns,moby",
    javaOptions in IntegrationTest += "-Dsun.net.spi.nameservice.provider.2=default",

    jacoco.settings,
    itJacoco.settings,

    fork in itJacoco.Config := true,
    javaOptions in itJacoco.Config += "-Dsun.net.spi.nameservice.provider.1=dns,moby",
    javaOptions in itJacoco.Config += "-Dsun.net.spi.nameservice.provider.2=default"
  )
