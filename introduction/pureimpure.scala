trait Operations {
  def product(x: Int, y: Int) = x * y
}

trait Console {
  import scala.io.Source

  def getNumberFromUser(): Int = {
    println("Enter an integer:")
    Source.stdin.getLines.next.toInt
  }

  def showResult(operation: String, result: Int): Unit =
    println(s"The ${operation} is: ${result}")
}

object Productifier extends App with Operations with Console {
  
  val x = getNumberFromUser()
  val y = getNumberFromUser()
  val result = product(x, y)
  showResult("product", result)
}

