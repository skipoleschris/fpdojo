
object Model {

  type Position = (Int, Int)

  private def column(p: Position) = p._1
  private def row(p: Position) = p._2

  type Row = IndexedSeq[Boolean]
  type Columns = IndexedSeq[Row]
  case class Universe(width: Int, height: Int, columns: Columns) 

  def at(p: Position, u: Universe): Boolean = u.columns(column(p)).apply(row(p))  
}

trait Rules {

  import Model._

  def universeFrom(width: Int, height: Int, alivePositions: Set[Position]) = {
    val grid = (for (column <- 0 until width) yield {
      (for {
        row <- 0 until height
        p = (column, row)
      } yield alivePositions contains p)
    })
    new Universe(width, height, grid)
  }

  private def lifeAdjacentTo(position: Position, universe: Universe): Int = {
    def wrap(i: Int, max: Int) = if (i < 0) max - 1 else if (i >= max) 0 else i

    (for {
      x <- (position._1 - 1) to (position._1 + 1)
      column = wrap(x, universe.width)
      y <- (position._2 - 1) to (position._2 + 1) 
      row = wrap(y, universe.height)
      p = (column, row) if (p != position)
    } yield at(p, universe)) count identity
  }

  private def isAlive(currentlyAlive: Boolean, adjacentLife: Int): Boolean = 
    if ( (adjacentLife == 2 || adjacentLife == 3) && currentlyAlive ) true
    else if ( adjacentLife == 3 && !currentlyAlive ) true
    else false

  def tick(universe: Universe): Universe = {
    val nextAlivePositions = for {
      column <- 0 until universe.width
      row <- 0 until universe.height
      p = (column, row)
      if (isAlive(at(p, universe), lifeAdjacentTo(p, universe)))
    } yield p
    universeFrom(universe.width, universe.height, nextAlivePositions.toSet)
  }
}

trait UI {
  import Model._
  import javax.swing._
  import java.awt._

  private val panel = new JPanel()

  def displayWindow(width: Int, height: Int): Unit = {
    val frame = new JFrame("Game of Life")
    panel.setSize(new Dimension(10 * width, 10 * height))
    frame.add(panel)
    frame.pack()
    frame.setSize(new Dimension(10 * width, 10 * height))
    frame.setVisible(true)
  }

  def render(universe: Universe): Unit = {
    val graphics = panel.getGraphics
    for {
      column <- 0 until universe.width;
      row <- 0 until universe.height;
      p = (column, row)
    } {
      if ( at(p, universe) ) graphics.setColor(Color.RED) else graphics.setColor(Color.BLACK)
      graphics.fillRect(column * 10, row * 10, 10, 10)
    }
  }
}

object GameOfLife extends Rules with UI {
  import Model._

  def start = {
    displayWindow(25, 25)
//    nextUniverse(universeFrom(25, 25, Set((10, 10), (10, 11), (10, 12), (11, 11), (5, 5), (5, 6), (6, 6), (20, 20), (20, 21), (21, 21), (22, 21))))
    nextUniverse(universeFrom(25, 25, Set((1,3), (2,3), (3,3), (3,2), (2,1))))
  }

  @scala.annotation.tailrec
  private def nextUniverse(universe: Universe): Unit = {
    render(universe)
    Thread.sleep(200L)
    val next = tick(universe)
    if (next != universe) nextUniverse(next)
  }
}

GameOfLife.start

