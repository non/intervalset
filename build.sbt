import scoverage.ScoverageSbtPlugin.ScoverageKeys._

name := "intervalset"

scalaVersion := "2.11.4"

version := "0.1-SNAPSHOT"

libraryDependencies += "org.spire-math" %% "spire" % "0.9.0"

libraryDependencies += "org.spire-math" %% "spire-scalacheck-binding" % "0.9.0" % "test"

libraryDependencies += "junit" % "junit" % "4.11" % "test"

libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.11.6" % "test"

unmanagedBase in Test <<= baseDirectory { base => base / "test-lib" }

coverageMinimum := 100

coverageFailOnMinimum := true

scalacOptions ++= Seq("-unchecked", "-feature")

initialCommands in console += """
import com.rklaehn.interval._
import spire.math._
import spire.implicits._
"""