package com.equalexperts.connect4

import org.specs2.Specification
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.scalacheck._

import unfiltered.Async
import unfiltered.response._

class RestModuleControllerSpec extends Specification with ScalaCheck with TestableRestModuleController { def is =

  "Specification for the Connect4 REST Module Controller"                             ^
                                                                                      endp^
  "Pinging the default controller should"                                             ^
    "respond with the credits json"                                                   ! ping^
                                                                                      end

  import Prop.forAll
  import Arbitrary.arbitrary

  def ping = {
    val responder = new TestResponder()
    DefaultController.ping(responder)

    responder.awaitResponse
    responder.response.status must_== 200
    responder.response.body must contain(credits)
  }
}

trait TestableRestModuleController extends RestModule with PlayersModule {
  def showCredits(respond: RespondHandler) = respond ! CreditsResponse(credits)
  def startGame(respond: RespondHandler) = {}
  def registerPlayer(id: Long, respond: RespondHandler) = {}
  def checkStatus(id: Long, playerId: PlayerId, respond: RespondHandler) = {}
  def placePiece(id: Long, playerId: PlayerId, column: Int, respond: RespondHandler) = {}
  def endGames() = {}
}
