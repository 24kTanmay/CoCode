package com.cocode.websocket

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.cocode.services.CollaborationService
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class WebSocketHandler(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) {
  
  private val collaborationService = new CollaborationService()
  
  // WebSocket message types
  case class WSMessage(
    messageType: String,
    sessionId: Option[String] = None,
    userId: Option[String] = None,
    data: Map[String, String] = Map.empty
  )
  
  implicit val wsMessageFormat = jsonFormat4(WSMessage)
  
  def websocketRoute(sessionId: String): Route = {
    handleWebSocketMessages(websocketFlow(sessionId))
  }
  
  private def websocketFlow(sessionId: String): Flow[Message, Message, NotUsed] = {
    // Create a source that will emit messages to this specific session
    val (outgoing, outgoingSource) = Source.queue[Message](100, OverflowStrategy.dropHead).preMaterialize()
    
    // Handle incoming messages
    val incoming = Flow[Message].mapAsync(1) {
      case tm: TextMessage =>
        tm.toStrict(scala.concurrent.duration.FiniteDuration(5, "seconds")).map { strictText =>
          handleIncomingMessage(strictText.text, sessionId, outgoing)
          TextMessage("ack")
        }
      case _ =>
        Future.successful(TextMessage("unsupported message type"))
    }
    
    // Combine incoming and outgoing flows
    Flow.fromSinkAndSource(
      incoming.to(Sink.ignore),
      outgoingSource
    )
  }
  
  private def handleIncomingMessage(messageText: String, sessionId: String, outgoing: akka.stream.scaladsl.SourceQueueWithComplete[Message]): Unit = {
    try {
      val message = messageText.parseJson.convertTo[WSMessage]
      
      message.messageType match {
        case "cursor_update" =>
          val fileId = message.data.getOrElse("fileId", "")
          val line = message.data.getOrElse("line", "0").toInt
          val column = message.data.getOrElse("column", "0").toInt
          val userId = message.userId.getOrElse("")
          
          collaborationService.updateCursor(sessionId, userId, fileId, line, column).onComplete {
            case Success(true) =>
              // Broadcast cursor update to other users in the same project
              broadcastToProject(sessionId, message)
            case _ => // Handle error
          }
          
        case "text_change" =>
          val fileId = message.data.getOrElse("fileId", "")
          val operation = message.data.getOrElse("operation", "")
          val position = message.data.getOrElse("position", "0").toInt
          val content = message.data.getOrElse("content", "")
          val userId = message.userId.getOrElse("")
          
          collaborationService.recordTextChange(sessionId, userId, fileId, operation, position, content).onComplete {
            case Success(true) =>
              // Broadcast text change to other users in the same project
              broadcastToProject(sessionId, message)
            case _ => // Handle error
          }
          
        case "ping" =>
          val pongMessage = WSMessage("pong", Some(sessionId))
          outgoing.offer(TextMessage(pongMessage.toJson.toString))
          
        case _ =>
          println(s"Unknown message type: ${message.messageType}")
      }
    } catch {
      case e: Exception =>
        println(s"Error parsing WebSocket message: ${e.getMessage}")
        val errorMessage = WSMessage("error", Some(sessionId), data = Map("message" -> "Invalid message format"))
        outgoing.offer(TextMessage(errorMessage.toJson.toString))
    }
  }
  
  private def broadcastToProject(senderSessionId: String, message: WSMessage): Unit = {
    // TODO: Implement actual broadcasting to all sessions in the same project
    // This would require maintaining a registry of active WebSocket connections
    // grouped by project ID
    println(s"Broadcasting message from session $senderSessionId: ${message.messageType}")
  }
}
