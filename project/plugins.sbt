resolvers += Resolver.url("dialog-sbt-plugins", url("https://dl.bintray.com/dialog/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.trueaccord.scalapb" % "sbt-scalapb" % "0.5.34")

// libraryDependencies ++= Seq("com.github.os72" % "protoc-jar" % "3.0.0-b2")

addSbtPlugin("im.dlg" % "sbt-dialog-houserules" % "0.1.13")
