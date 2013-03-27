package com.equalexperts.connect4

trait PlayersModule extends GameModule {

  sealed trait GameResponse
  case class CreditsResponse(credits: String) extends GameResponse

  trait RespondHandler {
    def !(response: GameResponse): Unit
  }

  def showCredits(respond: RespondHandler): Unit
  def endGames(): Unit
}

trait ActorBasedPlayersModule extends PlayersModule {
  import akka.actor.Actor
  import akka.actor.ActorRef
  import akka.actor.ActorSystem
  import akka.actor.Props

  def showCredits(respond: RespondHandler): Unit =
    actors.connect4 ! ShowCreditsEvent(respond)

  def endGames(): Unit = actors.shutdown()

  private object actors {
    private val factory = actorSystemFactory
    val actorSystem = factory.actorSystem
    val connect4 = factory.connect4
    def shutdown(): Unit = actorSystem.shutdown()
  }

  protected trait ActorSystemFactory {
    def actorSystem: ActorSystem
    def connect4: ActorRef
  } 

  protected def actorSystemFactory = new ActorSystemFactory {
    val actorSystem = ActorSystem("connect4")
    val connect4 = actorSystem.actorOf(Props(new GameStartupActor()), name = "Connect4")
  }

  private abstract class GameEvent {
    def respond: RespondHandler
  }
  private case class ShowCreditsEvent(respond: RespondHandler) extends GameEvent

  protected class GameStartupActor extends Actor {
    def receive = {
      case ShowCreditsEvent(respond) =>
        respond ! CreditsResponse(credits)
    }
  }
}
