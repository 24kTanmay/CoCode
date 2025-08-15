package com.cocode.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.SourceQueueWithComplete
import com.cocode.websocket.WebSocketManager.{ConnectionId, UserId}

object ConnectionActor {
  
  // Commands
  sealed trait Command
  case class SendMessage(message: String) extends Command
  case class HandleIncomingMessage(message: String) extends Command
  case class ConnectionClosed() extends Command
  
  def apply(
    connectionId: ConnectionId,
    userId: UserId,
    projectId: String,
    outgoing: SourceQueueWithComplete[Message],
    collaborationManager: ActorRef[CollaborationManager.Command]
  ): Behavior[Command] = {
    Behaviors.setup { context =>
      context.log.info(s"Connection actor started for user $userId in project $projectId")
      
      Behaviors.receiveMessage {
        case SendMessage(message) =>
          outgoing.offer(TextMessage(message))
          Behaviors.same
          
        case HandleIncomingMessage(messageText) =>
          context.log.debug(s"Received message from $userId: $messageText")
          
          try {
            import spray.json._
            import spray.json.DefaultJsonProtocol._
            
            val json = messageText.parseJson.asJsObject
            val messageType = json.fields.get("type").map(_.convertTo[String]).getOrElse("unknown")
            
            messageType match {
              case "cursor_move" =>
                // Broadcast cursor movement to other users
                val broadcastMessage = json.copy(
                  fields = json.fields + ("userId" -> JsString(userId)) + ("timestamp" -> JsString(java.time.Instant.now().toString))
                )
                collaborationManager ! CollaborationManager.BroadcastToProject(
                  projectId, 
                  broadcastMessage.toString, 
                  Some(connectionId)
                )
                
              case "text_change" =>
                // Broadcast text changes to other users
                val broadcastMessage = json.copy(
                  fields = json.fields + ("userId" -> JsString(userId)) + ("timestamp" -> JsString(java.time.Instant.now().toString))
                )
                collaborationManager ! CollaborationManager.BroadcastToProject(
                  projectId, 
                  broadcastMessage.toString, 
                  Some(connectionId)
                )
                
              case "selection_change" =>
                // Broadcast selection changes to other users
                val broadcastMessage = json.copy(
                  fields = json.fields + ("userId" -> JsString(userId)) + ("timestamp" -> JsString(java.time.Instant.now().toString))
                )
                collaborationManager ! CollaborationManager.BroadcastToProject(
                  projectId, 
                  broadcastMessage.toString, 
                  Some(connectionId)
                )
                
              case "ping" =>
                // Respond with pong
                val pongMessage = s"""{"type":"pong","timestamp":"${java.time.Instant.now()}"}"""
                outgoing.offer(TextMessage(pongMessage))
                
              case "user_typing" =>
                // Broadcast typing indicator
                val broadcastMessage = json.copy(
                  fields = json.fields + ("userId" -> JsString(userId)) + ("timestamp" -> JsString(java.time.Instant.now().toString))
                )
                collaborationManager ! CollaborationManager.BroadcastToProject(
                  projectId, 
                  broadcastMessage.toString, 
                  Some(connectionId)
                )
                
              case _ =>
                context.log.warn(s"Unknown message type: $messageType")
                val errorMessage = s"""{"type":"error","message":"Unknown message type: $messageType"}"""
                outgoing.offer(TextMessage(errorMessage))
            }
          } catch {
            case e: Exception =>
              context.log.error(s"Error processing message from $userId: ${e.getMessage}")
              val errorMessage = s"""{"type":"error","message":"Invalid message format"}"""
              outgoing.offer(TextMessage(errorMessage))
          }
          
          Behaviors.same
          
        case ConnectionClosed() =>
          context.log.info(s"Connection closed for user $userId in project $projectId")
          collaborationManager ! CollaborationManager.LeaveProject(projectId, connectionId)
          Behaviors.stopped
      }
    }
  }
}
