package com.cocode.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.cocode.models.{Project, CodeFile}
import com.cocode.services.{ProjectService, AuthService}
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import java.time.Instant

class ProjectRoutes {
  
  private val projectService = new ProjectService()
  private val authService = new AuthService()
  
  // JSON formats
  implicit val instantFormat = new JsonFormat[Instant] {
    def write(instant: Instant) = JsString(instant.toString)
    def read(value: JsValue) = value match {
      case JsString(s) => Instant.parse(s)
      case _ => throw DeserializationException("Expected ISO instant string")
    }
  }
  
  implicit val projectFormat = jsonFormat7(Project)
  implicit val codeFileFormat = jsonFormat8(CodeFile)
  
  case class CreateProjectRequest(name: String, description: Option[String])
  case class UpdateFileRequest(content: String)
  
  implicit val createProjectRequestFormat = jsonFormat2(CreateProjectRequest)
  implicit val updateFileRequestFormat = jsonFormat1(UpdateFileRequest)
  
  val routes: Route = 
    pathPrefix("projects") {
      headerValueByName("Authorization") { token =>
        onSuccess(authService.validateToken(token)) {
          case Some(userId) =>
            pathEndOrSingleSlash {
              get {
                // Get all projects for user
                onSuccess(projectService.getUserProjects(userId)) { projects =>
                  complete(projects)
                }
              } ~
              post {
                // Create new project
                entity(as[CreateProjectRequest]) { request =>
                  onSuccess(projectService.createProject(request.name, request.description, userId)) { project =>
                    complete(StatusCodes.Created, project)
                  }
                }
              }
            } ~
            path(Segment) { projectId =>
              get {
                // Get specific project
                onSuccess(projectService.getProject(projectId, userId)) {
                  case Some(project) => complete(project)
                  case None => complete(StatusCodes.NotFound, "Project not found")
                }
              } ~
              delete {
                // Delete project
                onSuccess(projectService.deleteProject(projectId, userId)) { success =>
                  if (success) complete(StatusCodes.NoContent)
                  else complete(StatusCodes.NotFound, "Project not found")
                }
              }
            } ~
            path(Segment / "files") { projectId =>
              get {
                // Get all files in project
                onSuccess(projectService.getProjectFiles(projectId, userId)) { files =>
                  complete(files)
                }
              } ~
              post {
                // Create new file
                parameters("path", "name", "language") { (path, name, language) =>
                  onSuccess(projectService.createFile(projectId, path, name, language, userId)) { file =>
                    complete(StatusCodes.Created, file)
                  }
                }
              }
            } ~
            path(Segment / "files" / Segment) { (projectId, fileId) =>
              get {
                // Get specific file
                onSuccess(projectService.getFile(fileId, projectId, userId)) {
                  case Some(file) => complete(file)
                  case None => complete(StatusCodes.NotFound, "File not found")
                }
              } ~
              put {
                // Update file content
                entity(as[UpdateFileRequest]) { request =>
                  onSuccess(projectService.updateFileContent(fileId, request.content, userId)) { success =>
                    if (success) complete(StatusCodes.OK, "File updated")
                    else complete(StatusCodes.NotFound, "File not found")
                  }
                }
              } ~
              delete {
                // Delete file
                onSuccess(projectService.deleteFile(fileId, projectId, userId)) { success =>
                  if (success) complete(StatusCodes.NoContent)
                  else complete(StatusCodes.NotFound, "File not found")
                }
              }
            }
          case None =>
            complete(StatusCodes.Unauthorized, "Invalid token")
        }
      }
    }
}
