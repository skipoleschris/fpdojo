package com.equalexperts.connect4

import org.specs2.Specification
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.scalacheck._

import unfiltered.Async
import unfiltered.response._

class RestModuleSpec extends Specification with ScalaCheck with TestableRestModule { def is =

  "Specification for the Connect4 REST Module"                                        ^
                                                                                      endp^
  "Pinging the default controller should"                                             ^
    "respond with the credits json"                                                   ! ping^
                                                                                      endp^
  "Making calls to the REST interface should"                                         ^
    "answer a GET request on the ping URL"                                            ! getPing^                                                         
                                                                                      end

  import Prop.forAll
  import Arbitrary.arbitrary

  def ping = {
    val responder = new TestResponder()
    DefaultController.ping(responder)

    responder.latch.await
    responder.response.status must_== 200
    responder.response.body must contain(credits)
  }

  def getPing = {
    //TODO: make an HTTP request to the netty server and get the response
    pending
  }
}

trait TestableRestModule extends RestModule with PlayersModule {
  def showCredits(respond: RespondHandler) = { respond ! CreditsResponse(credits) }
  def endGames() = {}

  protected override def controller = new Controller {
    def ping[A](req: Async.Responder[A]) = { req respond Ok }
    def notAllowed[A](req: Async.Responder[A]) = {}
  }
}
