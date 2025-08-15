package com.cocode.services

import com.cocode.models.User
import com.cocode.routes.AuthRoutes.AuthResponse
import org.mindrot.jbcrypt.BCrypt
import pdi.jwt.{JwtAlgorithm, JwtSprayJson}
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class AuthService(implicit ec: ExecutionContext) {
  
  private val secretKey = "your-secret-key" // TODO: Move to config
  private val algorithm = JwtAlgorithm.HS256
  
  // TODO: Replace with actual database operations
  private var users: Map[String, User] = Map.empty
  private var blacklistedTokens: Set[String] = Set.empty
  
  implicit val userFormat = jsonFormat6(User)
  
  def register(username: String, email: String, password: String): Future[Option[AuthResponse]] = {
    Future {
      if (users.values.exists(_.email == email)) {
        None // User already exists
      } else {
        val userId = UUID.randomUUID().toString
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        val user = User(
          id = userId,
          username = username,
          email = email,
          passwordHash = passwordHash,
          createdAt = Instant.now()
        )
        
        users = users + (userId -> user)
        val token = generateToken(userId)
        Some(AuthResponse(token, user.copy(passwordHash = ""))) // Don't return password hash
      }
    }
  }
  
  def login(email: String, password: String): Future[Option[AuthResponse]] = {
    Future {
      users.values.find(_.email == email) match {
        case Some(user) if BCrypt.checkpw(password, user.passwordHash) =>
          val token = generateToken(user.id)
          val updatedUser = user.copy(lastLoginAt = Some(Instant.now()))
          users = users + (user.id -> updatedUser)
          Some(AuthResponse(token, updatedUser.copy(passwordHash = "")))
        case _ => None
      }
    }
  }
  
  def logout(token: String): Future[Boolean] = {
    Future {
      blacklistedTokens = blacklistedTokens + token
      true
    }
  }
  
  def validateToken(token: String): Future[Option[String]] = {
    Future {
      if (blacklistedTokens.contains(token)) {
        None
      } else {
        Try {
          val claims = JwtSprayJson.decode(token, secretKey, Seq(algorithm))
          claims match {
            case Success(payload) =>
              val json = payload.parseJson.asJsObject
              json.fields.get("userId") match {
                case Some(JsString(userId)) => Some(userId)
                case _ => None
              }
            case _ => None
          }
        }.getOrElse(None)
      }
    }
  }
  
  private def generateToken(userId: String): String = {
    val claims = JsObject(
      "userId" -> JsString(userId),
      "iat" -> JsNumber(Instant.now().getEpochSecond),
      "exp" -> JsNumber(Instant.now().plusSeconds(86400).getEpochSecond) // 24 hours
    )
    JwtSprayJson.encode(claims, secretKey, algorithm)
  }
}
