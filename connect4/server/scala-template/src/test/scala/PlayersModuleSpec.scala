package com.equalexperts.connect4

import org.specs2.Specification
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.scalacheck._

class PlayersModuleSpec extends Specification with ScalaCheck with TestableActorBasedPlayersModule { def is =

  "Specification for the Connect4 Players Module Actor based implementation"          ^
                                                                                      endp^
  "Asking to show the credits should"                                                 ^
    "respond with the credits response to the responder"                              ! showCreditsRequest^
                                                                                      end

  import Prop.forAll
  import Arbitrary.arbitrary

  def showCreditsRequest = {
    val handler = new CapturingRespondHandler()
    showCredits(handler)

    handler.captured must_== CreditsResponse(credits)
  }
}

trait TestableActorBasedPlayersModule extends ActorBasedPlayersModule {
  import akka.actor.ActorRef
  import akka.actor.ActorSystem
  import akka.testkit.TestActorRef

  protected override def actorSystemFactory = new ActorSystemFactory {
    private val games = scala.collection.mutable.Map.empty[Long, ActorRef]

    implicit val actorSystem = ActorSystem("testing")
    val connect4 = TestActorRef(new GameStartupActor())
  }

  class CapturingRespondHandler extends RespondHandler {
    var captured: GameResponse = _
    def !(response: GameResponse) = captured = response 
  }
}