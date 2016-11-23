import im.dlg.DialogHouseRules._

scalaVersion := "2.12.4"

name := "akka-scalapb-serialization"
organization := "im.dlg"
organizationName := "Dialog LLC"
organizationHomepage := Some(new URL("https://dlg.im"))

defaultDialogSettings
mitLicense
scalapbSettings

val akkaV = "2.5.7"

resolvers += "mvnrepo" at "https://mvnrepository.com/artifact/"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"   % akkaV     % "provided",
  "com.typesafe.akka" %% "akka-slf4j"   % akkaV     % "provided",
  "com.google.guava"  %  "guava"        % "19.0",
  "com.google.code.findbugs" % "jsr305" % "3.0.1"
)