package com.equalexperts.connect4

trait GameModule {

  sealed trait Colour { def describe: String }
  trait Red extends Colour { val describe = "red" }
  trait Yellow extends Colour { val describe = "yellow" }

  sealed trait Piece extends Colour { def symbol: Char }
  case object RedPiece extends Piece with Red { val symbol = 'R' }
  case object YellowPiece extends Piece with Yellow { val symbol = 'Y' }

  protected val Width = 7
  protected val Height = 6

  sealed trait ErrorCode
  case object InvalidColumn extends ErrorCode { override def toString = "InvalidColumn" }
  case object ColumnFull extends ErrorCode { override def toString = "ColumnFull" }
  case object InvalidEvent extends ErrorCode { override def toString = "InvalidEvent" }
  case object UnknownGame extends ErrorCode { override def toString = "UnknownGame" }

  case class PlayerEvent(piece: Piece, column: Int) {
    require(column > 0 && column <= Width)
  }

  case class GameEvents[P <: Piece](events: Seq[PlayerEvent]) {
    def addEvent[B <: Piece](event: PlayerEvent): GameEvents[B] = copy(events = event +: events)
  }

  type Line = Seq[Option[Piece]]
  type Column = Line
  case class Grid(columns: Seq[Column])

  object game {
    def create: GameEvents[RedPiece.type] = new GameEvents(Nil)

    def placeRedPiece(column: Int, game: GameEvents[RedPiece.type]): Either[ErrorCode, GameEvents[YellowPiece.type]] =
      placePiece(column, game, RedPiece)

    def placeYellowPiece(column: Int, game: GameEvents[YellowPiece.type]): Either[ErrorCode, GameEvents[RedPiece.type]] =
      placePiece(column, game, YellowPiece)

    private def placePiece[A <: Piece, B <: Piece](column: Int, game: GameEvents[A], piece: A): Either[ErrorCode, GameEvents[B]] =
      if ( column < 1 || column > Width ) Left(InvalidColumn)
      else if ( isColumnFull(column, game)) Left(ColumnFull)
      else Right(game.addEvent(PlayerEvent(piece, column)))

    private def isColumnFull(column: Int, game: GameEvents[_]): Boolean =
      (game.events count (_.column == column)) >= Height

    def isFull(game: GameEvents[_]): Boolean = game.events.length == (Width * Height)

    def winner(game: GameEvents[_]): Option[Colour] = {
      def allSamePiece(group: Line) = group forall (_ == group.head)
      def extractColor(group: Line) = group.head getOrElse (throw new IllegalStateException())

      val lines = grid.allPossibleLines(grid.createFromEvents(game.events))
      val allGroups = lines flatMap (_ sliding 4)
      val winningGroups = allGroups filterNot (_ contains None) filter allSamePiece
      winningGroups.headOption map extractColor
    }
  }

  object grid {
    lazy val EmptyGrid = createFromEvents(Nil)

    def createFromEvents(events: Seq[PlayerEvent]): Grid = {
      def piece(event: PlayerEvent) = event.piece
      @scala.annotation.tailrec
      def pad(column: Column): Column = if ( column.size == Height ) column else pad(None +: column) 

      val columnEvents = (events groupBy (_.column)).withDefaultValue(Nil)
      Grid(for ( column <- 1 to Width ) yield {
        pad(columnEvents(column) map (piece _ andThen Option.apply))
      })
    }

    def allPossibleLines(grid: Grid): Seq[Line] = {
      @scala.annotation.tailrec
      def diagonalFrom(column: Int, row: Int, line: Line): Line = {
        if ( column >= Width || row >= Height ) line
        else diagonalFrom(column + 1, row + 1, line :+ grid.columns(column)(row))
      }

      val rows = extractRows(grid)
      val diagonalsFromSide = for ( row <- 0 until (Height - 3) ) yield (diagonalFrom(0, row, Seq.empty))
      val diagonalsFromBottom = for ( column <- 1 until (Width - 3) ) yield (diagonalFrom(column, 0, Seq.empty))
      grid.columns ++ rows ++ diagonalsFromSide ++ diagonalsFromBottom
    }

    def asStringRepresentation(grid: Grid): Seq[String] = {
      def rowToString(row: Seq[Option[Piece]]) = (row map renderPiece).mkString
      def renderPiece(piece: Option[Piece]) = piece map (_.symbol) getOrElse '.'
      extractRows(grid) map rowToString  
    } 

    private def extractRows(grid: Grid) = 
      for ( row <- 0 until Height ) yield (grid.columns flatMap (_ drop row take 1))
  }
}
