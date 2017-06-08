/*
 * Copyright 2017 Decipher Technology Studios LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt.Keys.javaOptions

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    organization := "com.deciphernow",
    name         := "franz",
    version      := "1.0.0-SNAPSHOT",

    scalaVersion := "2.11.11",

    pgpPassphrase := Some(sys.env("PGP_PASSPHRASE").toCharArray),
    pgpPublicRing := file(sys.env("PGP_PUBLIC_RING")),
    pgpSecretRing := file(sys.env("PGP_SECRET_RING")),

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
