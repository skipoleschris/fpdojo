package com.equalexperts.connect4

import org.specs2.Specification
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.scalacheck._


class GameModuleSpec extends Specification with ScalaCheck with GameModule { def is =

  "Specification for the Connect4 Game Module that encodes the game state and rules"  ^
                                                                                      endp^
  "Retrieving the credits should"                                                     ^
    "return the credit string"                                                        ! checkCredits^
                                                                                      endp^
  "Setting up a new game should"                                                      ^
    "create a game events object with an empty sequence of events"                    ! createGameEvents^
                                                                                      endp^
  "Placing a piece should"                                                            ^
    "increase the number of captured events by one"                                   ! placingPieceCapturesEvent^
    "retain the ordering that events were captured"                                   ! placingPieceRetainsEventOrdering^   
    "place alternative coloured pieces"                                               ! placingPieceAlternatesColours^
    "fail if the column is full"                                                      ! placingPieceFailsForFullColumn^
                                                                                      end

  import Prop.forAll
  import Arbitrary.arbitrary

  private val columns = Gen.choose(1, Width)
  private val nonFullColumnGames = Gen.oneOf(gameEventsWithNonFullColumns, gameEventsWithNonFullColumns, 
                                             gameEventsWithNonFullColumns, gameEventsWithNonFullColumns,
                                             gameEventsWithNonFullColumns, gameEventsWithNonFullColumns)
  private val allColumnsFullGame = Gen.value(gameEventsForAllColumnsFull)
  private val emptyGame = Gen.value(gameEventsForEmptyGame)

  def checkCredits = {
    credits must_== "Connect4 Server. (c)2013 Equal Experts Limited. All Rights Reserved."
  }

  def createGameEvents = {
    game.create must_== GameEvents[RedPiece.type](Nil)
  }      

  def placingPieceCapturesEvent = forAll(nonFullColumnGames, columns) { 
    (events: GameEvents[RedPiece.type], column: Int) =>

    val result = game.placeRedPiece(column, events).right.get
    (result.events.length must_== (events.events.length + 1)) and
    (result.events.head must_== PlayerEvent(RedPiece, column))
  }

  def placingPieceRetainsEventOrdering = forAll(nonFullColumnGames, columns) { 
    (events: GameEvents[RedPiece.type], column: Int) =>

    val result = game.placeRedPiece(column, events).right.get
    result.events.tail must_== events.events
  }

  def placingPieceAlternatesColours = forAll(emptyGame, columns) {
    (events: GameEvents[RedPiece.type], column: Int) =>

    val gameAfterRed = game.placeRedPiece(column, events).right.get
    val gameAfterYellow = game.placeYellowPiece(column, gameAfterRed).right.get
    val gameAfterSecondRed = game.placeRedPiece(column, gameAfterYellow).right.get

    (gameAfterSecondRed.events.length must_== 3) and
    (gameAfterSecondRed.events must_== Seq(PlayerEvent(RedPiece, column), PlayerEvent(YellowPiece, column), PlayerEvent(RedPiece, column)))
  }

  def placingPieceFailsForFullColumn = forAll(allColumnsFullGame, columns) {
    (events: GameEvents[RedPiece.type], column: Int) =>

    game.placeRedPiece(column, events) must_== Left(ColumnFull)
  }


  private def gameEventsWithNonFullColumns = {
    val random = new scala.util.Random(System.currentTimeMillis)
    var piece: Piece = YellowPiece
    val events = for {
      column <- 1 to Width
      _ <- 1 to random.nextInt(Height)
    } yield {
      piece = if (piece == RedPiece) YellowPiece else RedPiece
      PlayerEvent(piece, column)
    }
    GameEvents[RedPiece.type](events)
  }
  
  private def gameEventsForAllColumnsFull = {
    var piece: Piece = YellowPiece
    val events = for {
      column <- 1 to Width
      _ <- 1 to Height
    } yield {
      piece = if (piece == RedPiece) YellowPiece else RedPiece
      PlayerEvent(piece, column)
    }
    GameEvents[RedPiece.type](events)
  }

  private def gameEventsForEmptyGame = GameEvents[RedPiece.type](Nil)
}
