name := "slick-tryouts"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies ++= List(
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe.slick" %% "slick" % "2.1.0",
  "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
  "mysql" % "mysql-connector-java" % "5.1.32",
  "com.typesafe" % "config" % "1.2.1",
  "org.postgresql" % "postgresql" % "9.3-1102-jdbc41"
)
    