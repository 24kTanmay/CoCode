package com.cocode.services

import com.cocode.models.{Project, CodeFile}

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class ProjectService(implicit ec: ExecutionContext) {
  
  // TODO: Replace with actual database operations
  private var projects: Map[String, Project] = Map.empty
  private var files: Map[String, CodeFile] = Map.empty
  
  def createProject(name: String, description: Option[String], ownerId: String): Future[Project] = {
    Future {
      val projectId = UUID.randomUUID().toString
      val project = Project(
        id = projectId,
        name = name,
        description = description,
        ownerId = ownerId,
        collaborators = List.empty,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
      )
      projects = projects + (projectId -> project)
      project
    }
  }
  
  def getProject(projectId: String, userId: String): Future[Option[Project]] = {
    Future {
      projects.get(projectId).filter(project => 
        project.ownerId == userId || project.collaborators.contains(userId)
      )
    }
  }
  
  def getUserProjects(userId: String): Future[List[Project]] = {
    Future {
      projects.values.filter(project => 
        project.ownerId == userId || project.collaborators.contains(userId)
      ).toList
    }
  }
  
  def deleteProject(projectId: String, userId: String): Future[Boolean] = {
    Future {
      projects.get(projectId) match {
        case Some(project) if project.ownerId == userId =>
          projects = projects - projectId
          // Also delete all files in the project
          files = files.filter(_._2.projectId != projectId)
          true
        case _ => false
      }
    }
  }
  
  def addCollaborator(projectId: String, collaboratorId: String, ownerId: String): Future[Boolean] = {
    Future {
      projects.get(projectId) match {
        case Some(project) if project.ownerId == ownerId =>
          val updatedProject = project.copy(
            collaborators = project.collaborators :+ collaboratorId,
            updatedAt = Instant.now()
          )
          projects = projects + (projectId -> updatedProject)
          true
        case _ => false
      }
    }
  }
  
  def createFile(projectId: String, path: String, name: String, language: String, userId: String): Future[CodeFile] = {
    Future {
      val fileId = UUID.randomUUID().toString
      val file = CodeFile(
        id = fileId,
        projectId = projectId,
        path = path,
        name = name,
        content = "",
        language = language,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
      )
      files = files + (fileId -> file)
      file
    }
  }
  
  def getFile(fileId: String, projectId: String, userId: String): Future[Option[CodeFile]] = {
    Future {
      for {
        file <- files.get(fileId)
        project <- projects.get(projectId)
        if file.projectId == projectId && (project.ownerId == userId || project.collaborators.contains(userId))
      } yield file
    }
  }
  
  def getProjectFiles(projectId: String, userId: String): Future[List[CodeFile]] = {
    Future {
      projects.get(projectId) match {
        case Some(project) if project.ownerId == userId || project.collaborators.contains(userId) =>
          files.values.filter(_.projectId == projectId).toList
        case _ => List.empty
      }
    }
  }
  
  def updateFileContent(fileId: String, content: String, userId: String): Future[Boolean] = {
    Future {
      files.get(fileId) match {
        case Some(file) =>
          projects.get(file.projectId) match {
            case Some(project) if project.ownerId == userId || project.collaborators.contains(userId) =>
              val updatedFile = file.copy(
                content = content,
                updatedAt = Instant.now()
              )
              files = files + (fileId -> updatedFile)
              true
            case _ => false
          }
        case _ => false
      }
    }
  }
  
  def deleteFile(fileId: String, projectId: String, userId: String): Future[Boolean] = {
    Future {
      files.get(fileId) match {
        case Some(file) if file.projectId == projectId =>
          projects.get(projectId) match {
            case Some(project) if project.ownerId == userId || project.collaborators.contains(userId) =>
              files = files - fileId
              true
            case _ => false
          }
        case _ => false
      }
    }
  }
}
