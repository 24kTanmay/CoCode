package com.cocode.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.cocode.websocket.WebSocketManager.{ConnectionId, ProjectId, UserId}

object CollaborationManager {
  
  // Commands
  sealed trait Command
  case class JoinProject(projectId: ProjectId, userId: UserId, connectionId: ConnectionId, replyTo: ActorRef[ProjectSessionActor.Command]) extends Command
  case class LeaveProject(projectId: ProjectId, connectionId: ConnectionId) extends Command
  case class BroadcastToProject(projectId: ProjectId, message: String, excludeConnection: Option[ConnectionId] = None) extends Command
  case class GetProjectSessions(projectId: ProjectId, replyTo: ActorRef[ProjectSessionsResponse]) extends Command
  
  // Responses
  case class ProjectSessionsResponse(sessions: Map[ConnectionId, UserId])
  
  def apply(): Behavior[Command] = {
    Behaviors.setup { context =>
      var projectActors: Map[ProjectId, ActorRef[ProjectSessionActor.Command]] = Map.empty
      
      Behaviors.receiveMessage {
        case JoinProject(projectId, userId, connectionId, replyTo) =>
          val projectActor = projectActors.getOrElse(projectId, {
            val actor = context.spawn(ProjectSessionActor(projectId), s"project-$projectId")
            projectActors = projectActors + (projectId -> actor)
            actor
          })
          
          projectActor ! ProjectSessionActor.AddConnection(userId, connectionId, replyTo)
          Behaviors.same
          
        case LeaveProject(projectId, connectionId) =>
          projectActors.get(projectId) match {
            case Some(projectActor) =>
              projectActor ! ProjectSessionActor.RemoveConnection(connectionId)
            case None =>
              context.log.warn(s"Project $projectId not found when trying to remove connection $connectionId")
          }
          Behaviors.same
          
        case BroadcastToProject(projectId, message, excludeConnection) =>
          projectActors.get(projectId) match {
            case Some(projectActor) =>
              projectActor ! ProjectSessionActor.BroadcastMessage(message, excludeConnection)
            case None =>
              context.log.warn(s"Project $projectId not found for broadcasting")
          }
          Behaviors.same
          
        case GetProjectSessions(projectId, replyTo) =>
          projectActors.get(projectId) match {
            case Some(projectActor) =>
              projectActor ! ProjectSessionActor.GetSessions(replyTo)
            case None =>
              replyTo ! ProjectSessionsResponse(Map.empty)
          }
          Behaviors.same
      }
    }
  }
}
