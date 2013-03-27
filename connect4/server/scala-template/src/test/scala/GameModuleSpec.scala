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
                                                                                      end

  import Prop.forAll
  import Arbitrary.arbitrary

  def checkCredits = {
    credits must_== "Connect4 Server. (c)2013 Equal Experts Limited. All Rights Reserved."
  }
}
