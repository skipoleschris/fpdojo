
object Model {
  type Position = (Int, Int)
  case class Universe(width: Int, height: Int, lifeAt: Set[Position], allPositions: Seq[Position])
}

trait Rules {
  import Model._

  def universeFrom(width: Int, height: Int, lifeAt: Set[Position]): Universe = {
    val allPositions = for {
      column <- 0 until width
      row <- 0 until height
    } yield (column, row)
    Universe(width, height, lifeAt, allPositions)
  }

  def tick(universe: Universe): Universe = {
    val aliveCheck = alive(lifeAdjacentTo(universe), currentlyAlive(universe))(_)
    val newLifePositions = universe.allPositions.foldLeft(Set.empty[Position]) { (newLife, p) => if (aliveCheck(p)) newLife + p else newLife }
    universe.copy(lifeAt = newLifePositions)
  }

  private def currentlyAlive(universe: Universe)(position: Position): Boolean = universe.lifeAt contains position

  private def lifeAdjacentTo(universe: Universe)(position: Position) = {
    def adjacent(p: Position) = {
      val colDiff = Math.abs(position._1 - p._1)
      val rowDiff = Math.abs(position._2 - p._2)

      if ( p == position ) false
      else ((colDiff == 0 || colDiff == 1 || colDiff == universe.width - 1) && (rowDiff == 0 || rowDiff == 1 || rowDiff == universe.height - 1))     
    }

    universe.lifeAt count adjacent
  }

  private def alive(adjacentLifeCount: Position => Int, 
                    isCurrentlyAlive: Position => Boolean)
                   (position: Position): Boolean = {
    val count = adjacentLifeCount(position)
    val isAlive = isCurrentlyAlive(position)

    if ( (count == 2 || count == 3) && isAlive ) true
    else if ( count == 3 && !isAlive ) true
    else false
  }
}

trait UI {
  this: Rules =>

  import Model._
  import javax.swing._
  import java.awt._

  private val scale = 15
  private val panel = new JPanel()

  def displayWindow(width: Int, height: Int): Unit = {
    val frame = new JFrame("Game of Life")
    frame.add(panel)
    frame.setVisible(true)

    val insets = frame.getInsets
    frame.setSize(new Dimension(scale * width + insets.left + insets.right, scale * height + insets.top + insets.bottom))
  }

  def render(universe: Universe): Unit = {
    val graphics = panel.getGraphics
    graphics.setColor(Color.BLACK)
    graphics.fillRect(0, 0, universe.width * scale, universe.height * scale)
    graphics.setColor(Color.RED)
    universe.lifeAt foreach (p => graphics.fillRect(p._1 * scale, p._2 * scale, scale, scale))
  }
}

trait ConfigLoader {
  this: Rules =>

  import Model._
  import scala.io.Source

  def load: Universe = {
    def cellWithPosition(acc: Seq[(Char, (Int, Int))], line: (String, Int)): Seq[(Char, (Int, Int))] = {
      val (items, index) = line
      acc ++ (items.zipWithIndex map (item => (item._1 -> (index, item._2))))
    }

    val lines = (Source.fromFile("life.txt").getLines filterNot (line => line.startsWith("!") || line.trim.isEmpty)).toList
    val cellsWithIndex = lines.zipWithIndex.foldLeft(Seq.empty[(Char, (Int, Int))])(cellWithPosition)
    val aliveCells = cellsWithIndex filter (_._1 == 'o') map (_._2.swap)

    universeFrom(lines.head.length, lines.length, aliveCells.toSet)
  }
}

object GameOfLife extends Rules with ConfigLoader with UI {
  import Model._

  def start = {
    val startingUniverse = load
    displayWindow(startingUniverse.width, startingUniverse.height)
    loop(startingUniverse)
  }

  @scala.annotation.tailrec
  private def loop(universe: Universe): Unit = {
    render(universe)
    Thread.sleep(200L)
    val next = tick(universe)
    if (next != universe) loop(next)
  }
}

GameOfLife.start
