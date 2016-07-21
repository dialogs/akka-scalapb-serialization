import im.dlg.DialogHouseRules._

scalaVersion := "2.11.7"

name := "akka-scalapb-serialization"
organization := "im.dlg"
organizationName := "Dialog LLC"
organizationHomepage := Some(new URL("https://dlg.im"))

defaultDialogSettings
mitLicense
scalapbSettings

val akkaV = "2.4.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV % "provided"
)
