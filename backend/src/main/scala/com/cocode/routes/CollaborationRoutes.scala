package com.cocode.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.cocode.models.{UserSession, CollaborationEvent}
import com.cocode.services.{CollaborationService, AuthService}
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import java.time.Instant

class CollaborationRoutes {
  
  private val collaborationService = new CollaborationService()
  private val authService = new AuthService()
  
  // JSON formats
  implicit val instantFormat = new JsonFormat[Instant] {
    def write(instant: Instant) = JsString(instant.toString)
    def read(value: JsValue) = value match {
      case JsString(s) => Instant.parse(s)
      case _ => throw DeserializationException("Expected ISO instant string")
    }
  }
  
  implicit val userSessionFormat = jsonFormat5(UserSession)
  implicit val collaborationEventFormat = jsonFormat6(CollaborationEvent)
  
  case class JoinSessionRequest(projectId: String)
  case class CursorUpdateRequest(fileId: String, line: Int, column: Int)
  
  implicit val joinSessionRequestFormat = jsonFormat1(JoinSessionRequest)
  implicit val cursorUpdateRequestFormat = jsonFormat3(CursorUpdateRequest)
  
  val routes: Route = 
    pathPrefix("collaboration") {
      headerValueByName("Authorization") { token =>
        onSuccess(authService.validateToken(token)) {
          case Some(userId) =>
            path("sessions" / "join") {
              post {
                entity(as[JoinSessionRequest]) { request =>
                  onSuccess(collaborationService.joinSession(userId, request.projectId)) { session =>
                    complete(StatusCodes.Created, session)
                  }
                }
              }
            } ~
            path("sessions" / Segment / "leave") { sessionId =>
              post {
                onSuccess(collaborationService.leaveSession(sessionId, userId)) { success =>
                  if (success) complete(StatusCodes.OK, "Left session")
                  else complete(StatusCodes.NotFound, "Session not found")
                }
              }
            } ~
            path("sessions" / Segment / "cursor") { sessionId =>
              post {
                entity(as[CursorUpdateRequest]) { request =>
                  onSuccess(collaborationService.updateCursor(sessionId, userId, request.fileId, request.line, request.column)) { success =>
                    if (success) complete(StatusCodes.OK, "Cursor updated")
                    else complete(StatusCodes.BadRequest, "Failed to update cursor")
                  }
                }
              }
            } ~
            path("projects" / Segment / "sessions") { projectId =>
              get {
                onSuccess(collaborationService.getActiveSessions(projectId)) { sessions =>
                  complete(sessions)
                }
              }
            } ~
            path("projects" / Segment / "events") { projectId =>
              get {
                parameters("since".as[Long].?) { since =>
                  val sinceInstant = since.map(Instant.ofEpochMilli)
                  onSuccess(collaborationService.getRecentEvents(projectId, sinceInstant)) { events =>
                    complete(events)
                  }
                }
              }
            }
          case None =>
            complete(StatusCodes.Unauthorized, "Invalid token")
        }
      }
    }
}
