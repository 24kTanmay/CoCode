ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

lazy val root = (project in file("."))
  .settings(
    name := "cocode-backend",
    libraryDependencies ++= Seq(
      // Akka HTTP for REST APIs and WebSocket support
      "com.typesafe.akka" %% "akka-http" % "10.5.0",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.0",
      "com.typesafe.akka" %% "akka-actor-typed" % "2.8.0",
      "com.typesafe.akka" %% "akka-stream" % "2.8.0",
      "com.typesafe.akka" %% "akka-actor" % "2.8.0",
      
      // Database connectivity
      "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0",
      "org.postgresql" % "postgresql" % "42.6.0",
      "com.typesafe.slick" %% "slick" % "3.4.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
      
      // JSON handling
      "io.spray" %% "spray-json" % "1.3.6",
      "com.typesafe.play" %% "play-json" % "2.9.4",
      
      // Authentication and security
      "com.pauldijou" %% "jwt-spray-json" % "5.0.0",
      "org.mindrot" % "jbcrypt" % "0.4",
      
      // Configuration
      "com.typesafe" % "config" % "1.4.2",
      
      // Logging
      "ch.qos.logback" % "logback-classic" % "1.4.7",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
      
      // Testing
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "com.typesafe.akka" %% "akka-http-testkit" % "10.5.0" % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.8.0" % Test
    )
  )
