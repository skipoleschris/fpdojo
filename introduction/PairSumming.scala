object PairSumming {
  
  def sumPairs(pairs: List[(Int, Int)]): (Int, Int) = {

    @scala.annotation.tailrec
    def loop(pairs: List[(Int, Int)], 
             totals: (Int, Int)): (Int, Int) = pairs match {
      case Nil => totals
      case (left, right) :: xs =>
        val (totalLeft, totalRight) = totals
        loop(xs, (totalLeft + left, totalRight + right))
    }


    loop(pairs, (0, 0))
  }
}

val result = PairSumming.sumPairs((1, 6) :: (8, 5) :: (3, 2) :: (7, 1) :: Nil)
println(result)
