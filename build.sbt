name := "sarsa-sphero-desktop"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  // other dependencies here
  "org.scalanlp" %% "breeze" % "0.11.2",
  // native libraries are not included by default. add this if you want them (as of 0.7)
  // native libraries greatly improve performance, but increase jar sizes.
  // It also packages various blas implementations, which have licenses that may or may not
  // be compatible with the Apache License. No GPL code, as best I know.
  "org.scalanlp" %% "breeze-natives" % "0.11.2",
  // the visualization library is distributed separately as well.
  // It depends on LGPL code.
  "org.scalanlp" %% "breeze-viz" % "0.11.2",

  "com.typesafe.akka" %% "akka-actor" % "2.3.10",

  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",

  "org.slf4j" % "slf4j-log4j12" % "1.7.13",

  "log4j" % "log4j" % "1.2.17",

  // add the scalaz library
  "org.scalaz" %% "scalaz-core" % "7.1.0"


)

resolvers ++= Seq(
  // other resolvers here
  // if you want to use snapshot builds (currently 0.12-SNAPSHOT), use this.
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

unmanagedJars in Compile += file("lib/Sphero-Desktop-API.jar")

unmanagedJars in Compile += file("lib/au-id-cxd-math_2.10-1.0.jar")