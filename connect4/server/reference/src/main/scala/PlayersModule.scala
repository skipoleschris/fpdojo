package com.equalexperts.connect4

import java.util.UUID

trait PlayersModule extends GameModule {
  type PlayerId = String

  sealed trait GameResponse
  case class ErrorResponse(code: ErrorCode) extends GameResponse
  case class StartedResponse(id: Long) extends GameResponse
  case class RegisteredResponse(playerId: PlayerId, colour: Colour) extends GameResponse
  case class CheckResponse(grid: Grid, ready: Boolean = false, winner: Option[Colour] = None) extends GameResponse
  case class PlaceResponse(grid: Grid, winningMove: Boolean) extends GameResponse

  trait RespondHandler {
    def !(response: GameResponse): Unit
  }

  def startGame(respond: RespondHandler): Unit
  def registerPlayer(id: Long, respond: RespondHandler): Unit
  def checkStatus(id: Long, playerId: PlayerId, respond: RespondHandler): Unit
  def placePiece(id: Long, playerId: PlayerId, column: Int, respond: RespondHandler): Unit
  def endGames(): Unit

  protected def makePlayerId: PlayerId = UUID.randomUUID.toString
}

trait ActorBasedPlayersModule extends PlayersModule {
  import akka.actor.Actor
  import akka.actor.ActorRef
  import akka.actor.ActorSystem
  import akka.actor.Props

  def startGame(respond: RespondHandler): Unit =
    actors.connect4 ! NewGameEvent(respond)

  def registerPlayer(id: Long, respond: RespondHandler): Unit =
    actors.game(id) ! RegisterEvent(respond)

  def checkStatus(id: Long, playerId: PlayerId, respond: RespondHandler): Unit = 
    actors.game(id) ! CheckReady(playerId, respond)

  def placePiece(id: Long, playerId: PlayerId, column: Int, respond: RespondHandler): Unit =
    actors.game(id) ! PlacePiece(playerId, column, respond)

  def endGames(): Unit = actors.shutdown()

  private object actors {
    val actorSystem = ActorSystem("connect4")
    val connect4 = actorSystem.actorOf(Props(new GameStartupActor()), name = "Connect4")
    val unknownGame = actorSystem.actorOf(Props(new UnknownGameActor()), name = "Connect4_UnknownGame")
 
    def game(id: Long) = {
      val gameActor = actorSystem.actorFor(s"/user/Connect4_Game_${id}")
      if ( gameActor.isTerminated ) unknownGame
      else gameActor
    }
 
    def shutdown(): Unit = actorSystem.shutdown()
  }

  private abstract class GameEvent {
    def respond: RespondHandler
  }
  private case class NewGameEvent(respond: RespondHandler) extends GameEvent
  private case class RegisterEvent(respond: RespondHandler) extends GameEvent  
  private case class CheckReady(playerId: PlayerId, respond: RespondHandler) extends GameEvent
  private case class PlacePiece(playerId: PlayerId, column: Int, respond: RespondHandler) extends GameEvent

  private class GameStartupActor extends Actor {
    private var lastGameId: Long = 0

    def receive = {
      case NewGameEvent(respond) =>
        lastGameId = lastGameId + 1
        actors.actorSystem.actorOf(Props(new Connect4Actor(lastGameId)), name = s"Connect4_Game_${lastGameId}")
        respond ! StartedResponse(lastGameId)
    }
  }

  private class UnknownGameActor extends Actor {
    def receive = {
      case event: GameEvent => event.respond ! ErrorResponse(UnknownGame)
    }
  }

  private class Connect4Actor(id: Long) extends Actor {
    import context._

    def receive = awaitFirstRegistration

    private def awaitFirstRegistration: Receive = {
      case RegisterEvent(respond) => 
        respond ! RegisteredResponse(RedTurn.playerId, RedPiece)
        become(awaitSecondRegistration)
      case event: GameEvent => invalidEvent(event)
    }

    private def awaitSecondRegistration: Receive = {
      case RegisterEvent(respond) =>
        respond ! RegisteredResponse(YellowTurn.playerId, YellowPiece)
        become(move(game.create, RedTurn))
      case CheckReady(playerId, respond) if (playerId == RedTurn.playerId) =>
        respond ! CheckResponse(grid.EmptyGrid)      
      case event: GameEvent => invalidEvent(event)
    }

    private def move[A <: Piece, B <: Piece](gameEvents: GameEvents[A], turn: Turn[A, B]): Receive = {
      case CheckReady(playerId, respond) =>
        respond ! CheckResponse(grid.createFromEvents(gameEvents.events), playerId == turn.playerId)      
      case PlacePiece(playerId, column, respond) if (playerId == turn.playerId) =>
        turn.placePiece.apply(column, gameEvents) fold (errorCode(respond), gameUpdated(turn, respond))
      case event: GameEvent => invalidEvent(event)
    }

    private def gameUpdated[A <: Piece, B <: Piece](turn: Turn[A, B], respond: RespondHandler)(gameEvents: GameEvents[B]) = {
      val winner = game.winner(gameEvents)
      respond ! PlaceResponse(grid.createFromEvents(gameEvents.events), winner.isDefined)

      if ( winner.isDefined || game.isFull(gameEvents) ) become(gameComplete(gameEvents, winner))
      else become(move(gameEvents, turn.nextTurn))
    }

    private def gameComplete(gameEvents: GameEvents[_], winner: Option[Colour]): Receive = {
      case CheckReady(_, respond) =>
        respond ! CheckResponse(grid.createFromEvents(gameEvents.events), false, winner)
      case event: GameEvent => invalidEvent(event)
    }

    private sealed trait Turn[A <: Piece, B <: Piece] {
      def nextTurn: Turn[B, A]
      def playerId: PlayerId
      def placePiece: (Int, GameEvents[A]) => Either[ErrorCode, GameEvents[B]]
    }
    private object RedTurn extends Turn[RedPiece.type, YellowPiece.type] {
      def nextTurn = YellowTurn
      val playerId = makePlayerId
      val placePiece = game.placeRedPiece _
    }
    private object YellowTurn extends Turn[YellowPiece.type, RedPiece.type] { 
      def nextTurn = RedTurn
      val playerId = makePlayerId
      val placePiece = game.placeYellowPiece _
    }

    private def invalidEvent(event: GameEvent) = event.respond ! ErrorResponse(InvalidEvent)
    private def errorCode(respond: RespondHandler)(code: ErrorCode) = respond ! ErrorResponse(code)
  }
}
