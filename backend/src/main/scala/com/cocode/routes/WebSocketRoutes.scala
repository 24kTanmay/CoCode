package com.cocode.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.cocode.websocket.WebSocketHandler
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

class WebSocketRoutes(webSocketHandler: WebSocketHandler) {
  
  case class ConnectionsResponse(connections: Map[String, String])
  implicit val connectionsResponseFormat = jsonFormat1(ConnectionsResponse)
  
  val routes: Route = 
    pathPrefix("websocket") {
      path("projects" / Segment / "connections") { projectId =>
        get {
          onSuccess(webSocketHandler.getProjectConnections(projectId)) { connections =>
            complete(ConnectionsResponse(connections))
          }
        }
      }
    }
}
