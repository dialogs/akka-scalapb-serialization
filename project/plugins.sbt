resolvers += Resolver.url("dialog-sbt-plugins", url("https://dl.bintray.com/dialog/sbt-plugins"))(Resolver.ivyStylePatterns)

addSbtPlugin("im.dlg" % "sbt-dialog-houserules" % "0.1.17")
