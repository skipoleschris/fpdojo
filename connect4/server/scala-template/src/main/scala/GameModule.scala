package com.equalexperts.connect4

import scala.collection.immutable.Stack
import scala.util.Try


trait GameModule {
  var credits = "Connect4 Server. (c)2013 Equal Experts Limited. All Rights Reserved."

  sealed trait Piece
  case object RedPiece extends Piece
  case object YellowPiece extends Piece

  type Grid = Seq[Stack[Piece]]

  def createGame: Grid = {
    (1 to 7) map (_ => Stack.empty)
  }

  def addPiece(grid: Grid, piece: Piece, column: Int) : Try[Grid] =  Try {
    val columnIndex = column-1
    val targetColumn = grid(columnIndex)
    grid.updated(columnIndex, targetColumn push piece)
  }


}
