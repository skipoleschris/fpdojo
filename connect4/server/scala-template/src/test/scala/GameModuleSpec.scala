package com.equalexperts.connect4

import org.specs2.Specification
import org.specs2.ScalaCheck
import org.specs2.matcher.MatchResult
import org.scalacheck._

import scala.collection.immutable.Stack
import scala.util.Try
import scala.util.Success


class GameModuleSpec extends Specification with ScalaCheck with GameModule { def is =

  "Specification for the Connect4 Game Module that encodes the game state and rules"  ^
                                                                                      endp^
  "Retrieving the credits should"                                                     ^
    "return the credit string"                                                        ! checkCredits^
                                                                                      endp^
  "Creating a new connect 4 game instance should"                                     ^
    "return an empty game board"                                                      ! newGame^
                                                                                      endp^
  "A piece can be added to the board should"                                          ^
    "returns an updated board"                                                        ! firstPiece^
    "returns an error if move is invalid"                                             ! addInvalidPiece^
    "can be added after one is already present"                                       ! secondPiece^
                                                                                      end


  import Prop.forAll
  import Arbitrary.arbitrary

  def checkCredits = {
    credits must_== "Connect4 Server. (c)2013 Equal Experts Limited. All Rights Reserved."
  }

  def newGame = 
    createGame must_== Seq(Stack(), Stack(), Stack(), Stack(), Stack(), Stack(), Stack())

  def firstPiece = 
     addPiece(createGame, RedPiece, 3) must_== Success(Seq(Stack(), Stack(), Stack(RedPiece), Stack(), Stack(), Stack(), Stack()))

  def addInvalidPiece =
     addPiece(createGame, RedPiece, 1024).isFailure must beTrue 

  def secondPiece = {
    val result = for{
      first <- addPiece(createGame, RedPiece, 3)
      second <- addPiece(first, YellowPiece, 3) 
    } yield second
    
    result must_== Success(Seq(Stack(), Stack(), Stack(YellowPiece, RedPiece), Stack(), Stack(), Stack(), Stack()))
  }
}
