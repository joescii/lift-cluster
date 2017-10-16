organization in ThisBuild          := "net.liftmodules"
version in ThisBuild               := "0.0.2"
homepage in ThisBuild              := Some(url("https://github.com/joescii/lift-cluster"))
licenses in ThisBuild              += ("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
startYear in ThisBuild             := Some(2017)
organizationName in ThisBuild      := "WorldWide Conferencing, LLC"
crossScalaVersions in ThisBuild    := Seq("2.12.2", "2.11.11")
scalaVersion in ThisBuild          := (crossScalaVersions in ThisBuild).value.head


val liftVersion = SettingKey[String]("liftVersion", "Full version number of the Lift Web Framework")
val liftEdition = SettingKey[String]("liftEdition", "Lift Edition (short version number to append to artifact name)")

liftVersion in ThisBuild <<= liftVersion ?? "3.2.0-SNAPSHOT"
liftEdition in ThisBuild := liftVersion.value.replaceAllLiterally("-SNAPSHOT", "").split('.').take(2).mkString(".")


// Publishing stuff
publishTo in ThisBuild := { version.value.endsWith("SNAPSHOT") match {
  case true  => Some("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")
  case false => Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2")
}}

credentials in ThisBuild += Credentials( file("sonatype.credentials") )
credentials in ThisBuild += Credentials( file("/private/liftmodules/sonatype.credentials") )
publishMavenStyle in ThisBuild := true
publishArtifact in Test in ThisBuild := false
pomIncludeRepository in ThisBuild := { _ => false }
licenses in ThisBuild += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

pomExtra in ThisBuild := (
  <scm>
    <url>git@github.com:joescii/lift-cluster.git</url>
    <connection>scm:git:git@github.com:joescii/lift-cluster.git</connection>
  </scm>
    <developers>
      <developer>
        <id>joescii</id>
        <name>Joe Barnes</name>
        <url>https://github.com/joescii</url>
      </developer>
    </developers>
  )

val commonSettings = Seq(
  name := name.value + "_" + liftEdition.value,
  moduleName := name.value, // Necessary beginning with sbt 0.13, otherwise Lift editions get messed up.
  libraryDependencies ++= Seq(
    "net.liftweb"   %% "lift-webkit" % liftVersion.value % "provided",
    "org.scalatest" %% "scalatest"   % "3.0.0"           % "test"
  )
)

val common = Project("lift-cluster-common", file("./common"))
  .settings(commonSettings)

val kryo = Project("lift-cluster-kryo", file("./kryo"))
  .dependsOn(common)
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    "com.twitter" %% "chill" % "0.9.2" % "compile"
  ))

val jetty9 = Project("lift-cluster-jetty9", file("./jetty9"))
  .settings(commonSettings)
  .settings(libraryDependencies ++= Seq(
    "org.eclipse.jetty" % "jetty-webapp" % "9.0.7.v20131107" % "provided" // Using latest 9.0.x to ensure we're backwards-compatible
  ))

