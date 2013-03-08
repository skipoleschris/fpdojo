trait TwoDimensionsModule extends GameEngineModule {

  trait TwoDimensionsModule extends DimensionsModule {
    case class Dimension(width: Int, height: Int) extends DimensionLike {
      val allPositions = for {
        column <- 0 until width
        row <- 0 until height
      } yield Position(column, row)
    }
    
    case class Position(x: Int, y: Int) extends PositionLike {
      protected def adjacent(size: Dimension)(p: Position): Boolean = {
        val colDiff = Math.abs(x - p.x)
        val rowDiff = Math.abs(y - p.y)

        if ( p == this ) false
        else ((colDiff == 0 || colDiff == 1 || colDiff == size.width - 1) && (rowDiff == 0 || rowDiff == 1 || rowDiff == size.height - 1))     
      }
    }
  }

  trait TwoDimensionsRulesModule extends RulesModule with TwoDimensionsModule {
    case class Universe(size: Dimension, lifeAt: Set[Position]) extends UniverseLike {
      def allPositions = size.allPositions
      protected def updateLifeAt(newLifePositions: Set[Position]): Universe = copy(lifeAt = newLifePositions)
    }

    def universeFrom(size: Dimension, lifeAt: Set[Position]): Universe = Universe(size, lifeAt)

    protected def stayAlive(count: Int): Boolean = count == 2 || count == 3
    protected def beReborn(count: Int): Boolean = count == 3
  }

  trait TwoDimensionsUIModule extends UIModule with TwoDimensionsModule {
    import java.awt.{Color, Graphics}

    private val scale = 15

    protected def sizeOfPanel(size: Dimension) = (size.width * scale, size.height * scale)

    protected def clearPanel(graphics: Graphics, size: Dimension): Unit = {
      graphics setColor Color.BLACK
      graphics.fillRect(0, 0, size.width * scale, size.height * scale)
    }

    protected def drawPosition(graphics: Graphics, size: Dimension)(position: Position): Unit = {
      graphics setColor Color.RED
      graphics.fillRect(position.x * scale, position.y * scale, scale, scale)  
    }

    protected def orderForRendering(positions: Set[Position]): Seq[Position] = positions.toSeq

    protected def addFurniture(graphics: Graphics, size: Dimension): Unit = {}
  }

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
}

object TwoDimensionsGameOfLife extends App with GameEngineModule with TwoDimensionsModule {
  object GameOfLife extends GameLoopModule with TwoDimensionsRulesModule with TwoDimensionsUIModule with TwoDimensionsConfigLoaderModule
  GameOfLife.start(args(0))
}
