
// The following traits define the abstract modules that make up the
// game and hold the generic rules that apply no matter how the 
// game is represented or how many dimensions are utilised.
//
// Where possible all types are abstract and methods also abstract where
// they require knowledge of the underlying type implementation.
 
/**
 * Definition of the concept of dimensions and positions within
 * that dimension.
 */
trait DimensionsModule {
  type Dimension
  type Position <: PositionLike

  trait PositionLike { this: Position =>
    def adjacentTo(size: Dimension, cells: Set[Position]): Int = 
      cells count adjacent(size)

    protected def adjacent(size: Dimension)(p: Position): Boolean
  }
}

/**
 * Definition of the concept of a universe that is releated to dimensions
 * and positions. Encodes the concrete rules for the game but leaves
 * the exact specifics of the rules as abstract.
 */
trait RulesModule extends DimensionsModule {
  type Universe <: UniverseLike

  trait UniverseLike { this: Universe =>
    def size: Dimension
    def lifeAt: Set[Position]
    def allPositions: Seq[Position]
    def isAlive(position: Position): Boolean = lifeAt contains position
    protected def updateLifeAt(newLifePositions: Set[Position]): Universe

    def tick: Universe = {
      val aliveCheck = rules.alive(_.adjacentTo(size, lifeAt), isAlive)(_)
      val newLifePositions = allPositions.foldLeft(Set.empty[Position]) { (newLife, p) => if (aliveCheck(p)) newLife + p else newLife }
      updateLifeAt(newLifePositions)
    }
  }

  def universeFrom(size: Dimension, lifeAt: Set[Position]): Universe

  protected object rules {
    def alive(adjacentLifeCount: Position => Int, 
              isCurrentlyAlive: Position => Boolean)
             (position: Position): Boolean = {
      val count = adjacentLifeCount(position)
      val isAlive = isCurrentlyAlive(position)

      if ( isAlive && stayAlive(count) ) true
      else if ( !isAlive && beReborn(count) ) true
      else false
    }
  }

  protected def stayAlive(count: Int): Boolean
  protected def beReborn(count: Int): Boolean
}

/**
 * Definition of the UI dealing with displaying the window
 * and the rendering of the universe. Leaves the exact details
 * of sizing and rendering alive positions as abstract concepts.
 */
trait UIModule extends RulesModule {
  import javax.swing._
  import java.awt.Color
  import java.awt.Graphics

  object ui {
    private val panel = new JPanel()

    def displayWindow(size: Dimension): Unit = {
      val frame = new JFrame("Game of Life")
      frame.add(panel)
      frame.setVisible(true)

      val insets = frame.getInsets
      val (width, height) = sizeOfPanel(size)
      frame.setSize(new java.awt.Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom))
    }

    def render(universe: Universe): Unit = {
      val (width, height) = sizeOfPanel(universe.size)
      val graphics = panel.getGraphics
      graphics.setColor(Color.BLACK)
      clearPanel(graphics, universe.size)
      graphics.setColor(Color.RED)
      universe.lifeAt foreach drawPosition(graphics)
    }
  }

  protected def sizeOfPanel(size: Dimension): (Int, Int)
  protected def clearPanel(graphics: Graphics, size: Dimension): Unit
  protected def drawPosition(graphics: Graphics)(position: Position): Unit
}

/**
 * Definition of loading a universe from the "life.txt" file. Deals with
 * comment lines but leave the exact representation of the universe as an
 * abstract concept.
 */
trait ConfigLoaderModule extends RulesModule {
  object loader {
    import scala.io.Source

    def load: Universe = {
      def cellWithPosition(acc: Seq[(Char, (Int, Int))], line: (String, Int)): Seq[(Char, (Int, Int))] = {
        val (items, index) = line
        acc ++ (items.zipWithIndex map (item => (item._1 -> (index, item._2))))
      } 

      val lines = (Source.fromFile("life.txt").getLines filterNot (line => line.startsWith("!") || line.trim.isEmpty)).toList
      val (size, lifeAt) = parseUniverse(lines)
      universeFrom(size, lifeAt)
    }
  }

  protected def parseUniverse(lines: List[String]): (Dimension, Set[Position])
}

/**
 * The main game loop that pulls together all of the abstract traits in order
 * to build an initial universe from config, display a window and then render
 * each tick of the universe with a suitable delay.
 */
trait GameLoopModule extends RulesModule with UIModule with ConfigLoaderModule {
  def start = {
    val startingUniverse = loader.load
    ui.displayWindow(startingUniverse.size)
    loop(startingUniverse)
  }

  @scala.annotation.tailrec
  private def loop(u: Universe): Unit = {
    ui.render(u)
    Thread.sleep(200L)
    val next = u.tick
    if (next != u) loop(next)
  }
}



// The following traits implement a two dimensional version of the
// game of life.

/**
 * Implements the dimension and position concepts using two-dimensional
 * x and y coordinates.
 */
trait TwoDimensionsModule extends DimensionsModule {
  case class Dimension(width: Int, height: Int)
  
  case class Position(x: Int, y: Int) extends PositionLike {
    protected def adjacent(size: Dimension)(p: Position): Boolean = {
      val colDiff = Math.abs(x - p.x)
      val rowDiff = Math.abs(y - p.y)

      if ( p == this ) false
      else ((colDiff == 0 || colDiff == 1 || colDiff == size.width - 1) && (rowDiff == 0 || rowDiff == 1 || rowDiff == size.height - 1))     
    }
  }
}

/**
 * A concrete implementation of a two-dimensional universe.
 */
trait TwoDimensionsRulesModule extends RulesModule with TwoDimensionsModule {

  case class Universe(size: Dimension, lifeAt: Set[Position], allPositions: Seq[Position]) extends UniverseLike {
    protected def updateLifeAt(newLifePositions: Set[Position]): Universe = copy(lifeAt = newLifePositions)
  }

  def universeFrom(size: Dimension, lifeAt: Set[Position]): Universe = {
    val allPositions = for {
      column <- 0 until size.width
      row <- 0 until size.height
    } yield Position(column, row)
    Universe(size, lifeAt, allPositions)
  }

  protected def stayAlive(count: Int): Boolean = count == 2 || count == 3
  protected def beReborn(count: Int): Boolean = count == 3
}

/**
 * A concrete implementation of the functions for rendering positions
 * within a two-dimensional grid.
 */
trait TwoDimensionsUIModule extends UIModule with TwoDimensionsModule {
  import java.awt.Graphics

  private val scale = 15

  protected def sizeOfPanel(size: Dimension) = (size.width * scale, size.height * scale)

  protected def clearPanel(graphics: Graphics, size: Dimension): Unit = 
    graphics.fillRect(0, 0, size.width * scale, size.height * scale)

  protected def drawPosition(graphics: Graphics)(position: Position): Unit =
   graphics.fillRect(position.x * scale, position.y * scale, scale, scale)  
}

/**
 * A concrete implementation for parsing lines into a two-dimensional 
 * representation of the universe.
 */
trait TwoDimensionsConfigLoaderModule extends ConfigLoaderModule with TwoDimensionsModule { 
  protected def parseUniverse(lines: List[String]): (Dimension, Set[Position]) = {
    def cellWithPosition(acc: Seq[(Char, (Int, Int))], line: (String, Int)): Seq[(Char, (Int, Int))] = {
      val (items, index) = line
      acc ++ (items.zipWithIndex map (item => (item._1 -> (index, item._2))))
    } 

    val cellsWithIndex = lines.zipWithIndex.foldLeft(Seq.empty[(Char, (Int, Int))])(cellWithPosition)
    val aliveCells = cellsWithIndex filter (_._1 == 'o') map (_._2.swap) map (p => Position(p._1, p._2))
    (Dimension(lines.head.length, lines.length), aliveCells.toSet)
  }
}


// The final Game of Life that pulls together the game loop module with the
// two-dimensional implementation of each of the other modules.
object GameOfLife extends GameLoopModule with TwoDimensionsRulesModule with TwoDimensionsUIModule with TwoDimensionsConfigLoaderModule
GameOfLife.start
