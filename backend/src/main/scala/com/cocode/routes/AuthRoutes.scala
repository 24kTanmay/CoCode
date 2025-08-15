package com.cocode.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.cocode.models.User
import com.cocode.services.AuthService
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

class AuthRoutes {
  
  private val authService = new AuthService()
  
  // JSON formats
  implicit val userFormat = jsonFormat6(User)
  
  case class LoginRequest(email: String, password: String)
  case class RegisterRequest(username: String, email: String, password: String)
  case class AuthResponse(token: String, user: User)
  
  implicit val loginRequestFormat = jsonFormat2(LoginRequest)
  implicit val registerRequestFormat = jsonFormat3(RegisterRequest)
  implicit val authResponseFormat = jsonFormat2(AuthResponse)
  
  val routes: Route = 
    pathPrefix("auth") {
      path("register") {
        post {
          entity(as[RegisterRequest]) { request =>
            onSuccess(authService.register(request.username, request.email, request.password)) {
              case Some(authResponse) => complete(StatusCodes.Created, authResponse)
              case None => complete(StatusCodes.BadRequest, "Registration failed")
            }
          }
        }
      } ~
      path("login") {
        post {
          entity(as[LoginRequest]) { request =>
            onSuccess(authService.login(request.email, request.password)) {
              case Some(authResponse) => complete(authResponse)
              case None => complete(StatusCodes.Unauthorized, "Invalid credentials")
            }
          }
        }
      } ~
      path("logout") {
        post {
          headerValueByName("Authorization") { token =>
            onSuccess(authService.logout(token)) { success =>
              if (success) complete(StatusCodes.OK, "Logged out successfully")
              else complete(StatusCodes.BadRequest, "Logout failed")
            }
          }
        }
      }
    }
}
