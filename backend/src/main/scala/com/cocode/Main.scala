package com.cocode

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.cocode.actors.CollaborationManager
import com.cocode.routes.{AuthRoutes, CollaborationRoutes, ProjectRoutes}
import com.cocode.websocket.WebSocketHandler
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

object Main extends App with LazyLogging {
  
  implicit val system: ActorSystem = ActorSystem("cocode-system")
  implicit val typedSystem = system.toTyped
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  
  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")
  
  // Initialize collaboration manager
  val collaborationManager = typedSystem.systemActorOf(CollaborationManager(), "collaboration-manager")
  
  // Initialize routes
  val authRoutes = new AuthRoutes()
  val projectRoutes = new ProjectRoutes()
  val collaborationRoutes = new CollaborationRoutes()
  val webSocketHandler = new WebSocketHandler(collaborationManager)
  
  // Combine all routes
  val routes: Route = 
    pathPrefix("api" / "v1") {
      authRoutes.routes ~
      projectRoutes.routes ~
      collaborationRoutes.routes
    } ~
    path("ws" / "collaborate" / Segment) { projectId =>
      webSocketHandler.websocketRoute(projectId)
    } ~
    pathPrefix("health") {
      get {
        complete("OK")
      }
    }
  
  // Start the server
  val bindingFuture = Http().bindAndHandle(routes, host, port)
  
  logger.info(s"CoCode backend server started at http://$host:$port")
  logger.info("WebSocket endpoint available at ws://localhost:8080/ws/collaborate/{projectId}?token={jwt}")
  logger.info("Press RETURN to stop...")
  
  StdIn.readLine()
  
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
