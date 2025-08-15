package com.cocode.services

import com.cocode.models.{UserSession, CollaborationEvent}

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class CollaborationService(implicit ec: ExecutionContext) {
  
  // TODO: Replace with actual database operations
  private var sessions: Map[String, UserSession] = Map.empty
  private var events: List[CollaborationEvent] = List.empty
  
  def joinSession(userId: String, projectId: String): Future[UserSession] = {
    Future {
      val sessionId = UUID.randomUUID().toString
      val session = UserSession(
        sessionId = sessionId,
        userId = userId,
        projectId = projectId,
        connectedAt = Instant.now(),
        lastActivity = Instant.now()
      )
      
      sessions = sessions + (sessionId -> session)
      
      // Create join event
      val joinEvent = CollaborationEvent(
        eventType = "user_joined",
        sessionId = sessionId,
        userId = userId,
        projectId = projectId,
        data = Map("username" -> userId), // TODO: Get actual username
        timestamp = Instant.now()
      )
      events = joinEvent :: events
      
      session
    }
  }
  
  def leaveSession(sessionId: String, userId: String): Future[Boolean] = {
    Future {
      sessions.get(sessionId) match {
        case Some(session) if session.userId == userId =>
          sessions = sessions - sessionId
          
          // Create leave event
          val leaveEvent = CollaborationEvent(
            eventType = "user_left",
            sessionId = sessionId,
            userId = userId,
            projectId = session.projectId,
            data = Map("username" -> userId),
            timestamp = Instant.now()
          )
          events = leaveEvent :: events
          
          true
        case _ => false
      }
    }
  }
  
  def updateCursor(sessionId: String, userId: String, fileId: String, line: Int, column: Int): Future[Boolean] = {
    Future {
      sessions.get(sessionId) match {
        case Some(session) if session.userId == userId =>
          // Update last activity
          val updatedSession = session.copy(lastActivity = Instant.now())
          sessions = sessions + (sessionId -> updatedSession)
          
          // Create cursor event
          val cursorEvent = CollaborationEvent(
            eventType = "cursor_move",
            sessionId = sessionId,
            userId = userId,
            projectId = session.projectId,
            data = Map(
              "fileId" -> fileId,
              "line" -> line.toString,
              "column" -> column.toString
            ),
            timestamp = Instant.now()
          )
          events = cursorEvent :: events
          
          true
        case _ => false
      }
    }
  }
  
  def recordTextChange(sessionId: String, userId: String, fileId: String, operation: String, position: Int, content: String): Future[Boolean] = {
    Future {
      sessions.get(sessionId) match {
        case Some(session) if session.userId == userId =>
          // Update last activity
          val updatedSession = session.copy(lastActivity = Instant.now())
          sessions = sessions + (sessionId -> updatedSession)
          
          // Create text change event
          val textEvent = CollaborationEvent(
            eventType = "text_change",
            sessionId = sessionId,
            userId = userId,
            projectId = session.projectId,
            data = Map(
              "fileId" -> fileId,
              "operation" -> operation,
              "position" -> position.toString,
              "content" -> content
            ),
            timestamp = Instant.now()
          )
          events = textEvent :: events
          
          true
        case _ => false
      }
    }
  }
  
  def getActiveSessions(projectId: String): Future[List[UserSession]] = {
    Future {
      val cutoffTime = Instant.now().minusSeconds(300) // 5 minutes
      sessions.values.filter(session => 
        session.projectId == projectId && session.lastActivity.isAfter(cutoffTime)
      ).toList
    }
  }
  
  def getRecentEvents(projectId: String, since: Option[Instant]): Future[List[CollaborationEvent]] = {
    Future {
      val cutoff = since.getOrElse(Instant.now().minusSeconds(3600)) // Last hour by default
      events.filter(event => 
        event.projectId == projectId && event.timestamp.isAfter(cutoff)
      ).sortBy(_.timestamp.toEpochMilli)
    }
  }
  
  def cleanupInactiveSessions(): Future[Int] = {
    Future {
      val cutoffTime = Instant.now().minusSeconds(300) // 5 minutes
      val inactiveSessions = sessions.filter(_._2.lastActivity.isBefore(cutoffTime))
      
      // Create leave events for inactive sessions
      inactiveSessions.foreach { case (sessionId, session) =>
        val leaveEvent = CollaborationEvent(
          eventType = "user_left",
          sessionId = sessionId,
          userId = session.userId,
          projectId = session.projectId,
          data = Map("reason" -> "timeout"),
          timestamp = Instant.now()
        )
        events = leaveEvent :: events
      }
      
      sessions = sessions -- inactiveSessions.keys
      inactiveSessions.size
    }
  }
}
