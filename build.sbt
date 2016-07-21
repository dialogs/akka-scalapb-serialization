import com.trueaccord.scalapb.{ScalaPbPlugin => PB}
import sbtrelease._
import ReleaseStateTransformations._
import com.typesafe.sbt.pgp.PgpKeys._
import im.dlg.DialogHouseRules._

scalaVersion := "2.11.7"

name := "akka-scalapb-serialization"
organization := "im.dlg"
organizationName := "Dialog LLC"
organizationHomepage := Some(new URL("https://dlg.im"))

PB.protobufSettings

dialogDefaultSettings()

bintrayRepository := "akka-scalapb-serialization"

//PB.runProtoc in PB.protobufConfig := (args => com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray))

val akkaV = "2.4.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaV % "provided",
  "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.34" % "provided"
)
/*
dependencyOverrides ~= { overrides =>
  overrides + "com.google.protobuf" % "protobuf-java" % "3.0.0-beta-2"
}
*/