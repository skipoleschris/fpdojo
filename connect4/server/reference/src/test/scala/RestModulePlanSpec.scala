package com.equalexperts.connect4

import org.specs2.Specification
import org.specs2.specification.Step
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.scalacheck._

import unfiltered.Async
import unfiltered.response._

import java.net.URL
import java.net.HttpURLConnection

class RestModulePlanSpec extends Specification with ScalaCheck with Hosted with TestableRestModulePlan { def is =

                                                                                      sequential^
                                                                                      Step(startServer(Connect4Plan))^
  "Specification for the Connect4 REST Module Plan"                                   ^
                                                                                      endp^
  "Making calls to the REST interface should"                                         ^
    "answer a GET request on the ping URL"                                            ! getPing^ 
    "answer a POST request on the game create URL"                                    ! postGameCreate^
    "answer a POST request on the game register URL"                                  ! postGameRegister^
    "answer a GET request on the game status URL"                                     ! getGameStatus^
    "answer a POST request on the game place piece URL"                               ! postGamePlacePiece^
                                                                                      Step(shutdownServer())                                                        
                                                                                      end

  import Prop.forAll
  import Arbitrary.arbitrary

  def getPing = get("/connect4/ping")
  def postGameCreate = post("/connect4/game/create")
  def postGameRegister = post("/connect4/game/1/register")
  def getGameStatus = get("/connect4/game/1/player/99/status")
  def postGamePlacePiece = post("/connect4/game/1/player/99/placepiece/5")

  private def get(path: String) = callUrl(path, "GET")
  private def post(path: String) = callUrl(path, "POST")
  private def callUrl(path: String, method: String) = {
    val url = new URL(s"http://localhost:${port}${path}")
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]

    connection.setRequestMethod(method)
    val code = connection.getResponseCode()
    connection.disconnect()
    
    code must_== 200
  }
}

trait TestableRestModulePlan extends RestModule with PlayersModule {
  protected override def controller = new Controller {
    def ping[A](req: Async.Responder[A]) = req respond Ok 
    def notAllowed[A](req: Async.Responder[A]) = req respond Ok
    def handleCreateGame[A](req: Async.Responder[A]) = req respond Ok
    def handleRegisterPlayer[A](gameId: String)(req: Async.Responder[A]) = {
      if (gameId == "1" ) req respond Ok 
      else req respond BadRequest
    }
    def handleCheckStatus[A](gameId: String, playerId: String)(req: Async.Responder[A]) = {
      if (gameId == "1" && playerId == "99") req respond Ok
      else req respond BadRequest
    }
    def handlePlacePiece[A](gameId: String, playerId: String, column: String)(req: Async.Responder[A]) = {
      if (gameId == "1" && playerId == "99" && column == "5") req respond Ok
      else req respond BadRequest
    }
  }

  // Required but never called as part of these tests
  def showCredits(respond: RespondHandler) = {}
  def startGame(respond: RespondHandler) = {}
  def registerPlayer(id: Long, respond: RespondHandler) = {}
  def checkStatus(id: Long, playerId: PlayerId, respond: RespondHandler) = {}
  def placePiece(id: Long, playerId: PlayerId, column: Int, respond: RespondHandler) = {}
  def endGames() = {}
}
