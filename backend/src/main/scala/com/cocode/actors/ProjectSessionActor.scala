package com.cocode.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.cocode.websocket.WebSocketManager.{ConnectionId, ProjectId, UserId}

object ProjectSessionActor {
  
  // Commands
  sealed trait Command
  case class AddConnection(userId: UserId, connectionId: ConnectionId, connectionActor: ActorRef[ConnectionActor.Command]) extends Command
  case class RemoveConnection(connectionId: ConnectionId) extends Command
  case class BroadcastMessage(message: String, excludeConnection: Option[ConnectionId] = None) extends Command
  case class GetSessions(replyTo: ActorRef[CollaborationManager.ProjectSessionsResponse]) extends Command
  
  def apply(projectId: ProjectId): Behavior[Command] = {
    Behaviors.setup { context =>
      var connections: Map[ConnectionId, (UserId, ActorRef[ConnectionActor.Command])] = Map.empty
      
      Behaviors.receiveMessage {
        case AddConnection(userId, connectionId, connectionActor) =>
          connections = connections + (connectionId -> (userId, connectionActor))
          context.log.info(s"User $userId joined project $projectId with connection $connectionId")
          
          // Notify other users about new user joining
          val joinMessage = s"""{"type":"user_joined","userId":"$userId","connectionId":"$connectionId","timestamp":"${java.time.Instant.now()}"}"""
          connections.foreach { case (connId, (_, actor)) =>
            if (connId != connectionId) {
              actor ! ConnectionActor.SendMessage(joinMessage)
            }
          }
          
          Behaviors.same
          
        case RemoveConnection(connectionId) =>
          connections.get(connectionId) match {
            case Some((userId, _)) =>
              connections = connections - connectionId
              context.log.info(s"User $userId left project $projectId (connection $connectionId)")
              
              // Notify other users about user leaving
              val leaveMessage = s"""{"type":"user_left","userId":"$userId","connectionId":"$connectionId","timestamp":"${java.time.Instant.now()}"}"""
              connections.foreach { case (_, (_, actor)) =>
                actor ! ConnectionActor.SendMessage(leaveMessage)
              }
              
              // Stop this actor if no more connections
              if (connections.isEmpty) {
                context.log.info(s"No more connections in project $projectId, stopping actor")
                Behaviors.stopped
              } else {
                Behaviors.same
              }
            case None =>
              context.log.warn(s"Connection $connectionId not found in project $projectId")
              Behaviors.same
          }
          
        case BroadcastMessage(message, excludeConnection) =>
          connections.foreach { case (connId, (_, actor)) =>
            if (!excludeConnection.contains(connId)) {
              actor ! ConnectionActor.SendMessage(message)
            }
          }
          Behaviors.same
          
        case GetSessions(replyTo) =>
          val sessions = connections.map { case (connId, (userId, _)) => connId -> userId }
          replyTo ! CollaborationManager.ProjectSessionsResponse(sessions)
          Behaviors.same
      }
    }
  }
}
