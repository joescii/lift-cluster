organization in ThisBuild          := "com.joescii" // TODO switch to liftmodules once shippable
version in ThisBuild               := "0.0.1-SNAPSHOT"
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

val common = Project("lift-cluster-common", file("./common"))
  .settings(libraryDependencies ++= Seq(
    "net.liftweb"             %% "lift-webkit"    % liftVersion.value     % "provided",
    "org.scalatest"           %% "scalatest"      % "3.0.0"               % "test"
  ),
    name := name.value + "_" + liftEdition.value,
    moduleName := name.value // Necessary beginning with sbt 0.13, otherwise Lift editions get messed up.
  )

val kryo = Project("lift-cluster-kryo", file("./kryo"))
  .dependsOn(common)
  .settings(libraryDependencies ++= Seq(
    "net.liftweb"             %% "lift-webkit"    % liftVersion.value     % "provided",
    "com.twitter"             %% "chill"          % "0.9.2"               % "compile",
    "org.scalatest"           %% "scalatest"      % "3.0.0"               % "test"
  ),
    name := name.value + "_" + liftEdition.value,
    moduleName := name.value // Necessary beginning with sbt 0.13, otherwise Lift editions get messed up.
  )




