package com.cocode.models

import java.time.Instant

case class User(
  id: String,
  username: String,
  email: String,
  passwordHash: String,
  createdAt: Instant,
  lastLoginAt: Option[Instant] = None
)

case class Project(
  id: String,
  name: String,
  description: Option[String],
  ownerId: String,
  collaborators: List[String] = List.empty,
  createdAt: Instant,
  updatedAt: Instant
)

case class CodeFile(
  id: String,
  projectId: String,
  path: String,
  name: String,
  content: String,
  language: String,
  createdAt: Instant,
  updatedAt: Instant
)

case class CodeChange(
  id: String,
  fileId: String,
  userId: String,
  operation: String, // insert, delete, retain
  position: Int,
  content: String,
  timestamp: Instant
)

case class UserSession(
  sessionId: String,
  userId: String,
  projectId: String,
  connectedAt: Instant,
  lastActivity: Instant
)

case class CollaborationEvent(
  eventType: String, // user_joined, user_left, cursor_move, text_change
  sessionId: String,
  userId: String,
  projectId: String,
  data: Map[String, String],
  timestamp: Instant
)
