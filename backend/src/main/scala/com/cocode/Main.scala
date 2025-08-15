package com.cocode

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.cocode.routes.{AuthRoutes, CollaborationRoutes, ProjectRoutes}
import com.cocode.websocket.WebSocketHandler
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main extends App with LazyLogging {
  
  implicit val system: ActorSystem = ActorSystem("cocode-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  
  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  
  // Initialize routes
  val authRoutes = new AuthRoutes()
  val projectRoutes = new ProjectRoutes()
  val collaborationRoutes = new CollaborationRoutes()
  val webSocketHandler = new WebSocketHandler()
  
  // Combine all routes
  val routes: Route = 
    pathPrefix("api" / "v1") {
      authRoutes.routes ~
      projectRoutes.routes ~
      collaborationRoutes.routes
    } ~
    path("ws" / "collaborate" / Segment) { sessionId =>
      webSocketHandler.websocketRoute(sessionId)
    } ~
    pathPrefix("health") {
      get {
        complete("OK")
      }
    }
  
  // Start the server
  val bindingFuture = Http().bindAndHandle(routes, host, port)
  
  logger.info(s"CoCode backend server started at http://$host:$port")
  logger.info("Press RETURN to stop...")
  
  StdIn.readLine()
  
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
