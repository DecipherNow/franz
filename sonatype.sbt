sonatypeProfileName := "com.deciphernow"

publishMavenStyle := true

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

homepage := Some(url("https://github.com/deciphernow/franz"))

scmInfo := Some(ScmInfo(url("https://github.com/deciphernow/franz"), "scm:git@github.com:deciphernow/franz.git"))

developers := List(
  Developer(
    id="joshua",
    name="Joshua Mark Rutherford",
    email="joshua.rutherford@deciphernow.com",
    url=url("http://www.deciphernow.com"))
)

publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging)
