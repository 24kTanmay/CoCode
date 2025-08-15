package com.cocode.websocket

import akka.NotUsed
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.{ActorSystem => ClassicActorSystem}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.util.Timeout
import com.cocode.actors.{CollaborationManager, ConnectionActor, ProjectSessionActor}
import com.cocode.services.{AuthService, CollaborationService}
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class WebSocketHandler(
  collaborationManager: ActorRef[CollaborationManager.Command]
)(implicit 
  system: ClassicActorSystem, 
  typedSystem: ActorSystem[_],
  materializer: ActorMaterializer, 
  ec: ExecutionContext
) {
  
  private val collaborationService = new CollaborationService()
  private val authService = new AuthService()
  
  implicit val timeout: Timeout = 10.seconds
  
  // WebSocket message types
  case class WSMessage(
    messageType: String,
    sessionId: Option[String] = None,
    userId: Option[String] = None,
    data: Map[String, String] = Map.empty
  )
  
  implicit val wsMessageFormat = jsonFormat4(WSMessage)
  
  def websocketRoute(projectId: String): Route = {
    parameter("token") { token =>
      onComplete(authService.validateToken(token)) {
        case Success(Some(userId)) =>
          handleWebSocketMessages(websocketFlow(projectId, userId))
        case Success(None) =>
          complete("Unauthorized: Invalid token")
        case Failure(_) =>
          complete("Unauthorized: Token validation failed")
      }
    }
  }
  
  private def websocketFlow(projectId: String, userId: String): Flow[Message, Message, NotUsed] = {
    val connectionId = java.util.UUID.randomUUID().toString
    
    // Create a source that will emit messages to this specific connection
    val (outgoing, outgoingSource) = Source.queue[Message](100, OverflowStrategy.dropHead).preMaterialize()
    
    // Create connection actor
    val connectionActor = typedSystem.systemActorOf(
      ConnectionActor(connectionId, userId, projectId, outgoing, collaborationManager),
      s"connection-$connectionId"
    )
    
    // Register this connection with the collaboration manager
    collaborationManager ! CollaborationManager.JoinProject(projectId, userId, connectionId, connectionActor)
    
    // Handle incoming messages
    val incoming = Flow[Message].mapAsync(1) {
      case tm: TextMessage =>
        tm.toStrict(5.seconds).map { strictText =>
          connectionActor ! ConnectionActor.HandleIncomingMessage(strictText.text)
          TextMessage("") // Empty response, actual responses go through outgoing source
        }
      case _ =>
        Future.successful(TextMessage("""{"type":"error","message":"Unsupported message type"}"""))
    }
    
    // Combine incoming and outgoing flows
    Flow.fromSinkAndSourceCoupled(
      incoming.to(Sink.onComplete { _ =>
        connectionActor ! ConnectionActor.ConnectionClosed()
      }),
      outgoingSource
    )
  }
  
  // Get active connections for a project
  def getProjectConnections(projectId: String): Future[Map[String, String]] = {
    (collaborationManager ? CollaborationManager.GetProjectSessions(projectId, _)).map(_.sessions)
  }
}
}
