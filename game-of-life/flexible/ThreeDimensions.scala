trait ThreeDimensionsModule extends GameEngineModule {

  trait ThreeDimensionsModule extends DimensionsModule {
    case class Dimension(width: Int, height: Int, depth: Int) extends DimensionLike {
      val allPositions = for {
        column <- 0 until width
        row <- 0 until height
        plane <- 0 until depth
      } yield Position(column, row, plane)
    }
    
    case class Position(x: Int, y: Int, z: Int) extends PositionLike {
      protected def adjacent(size: Dimension)(p: Position): Boolean = {
        val colDiff = Math.abs(x - p.x)
        val rowDiff = Math.abs(y - p.y)
        val planeDiff = Math.abs(z - p.z)

        if ( p == this ) false
        else ((colDiff == 0 || colDiff == 1) && 
              (rowDiff == 0 || rowDiff == 1) &&
              (planeDiff == 0 || planeDiff == 1))     
      }
    }
  }

  trait ThreeDimensionsRulesModule extends RulesModule with ThreeDimensionsModule {
    case class Universe(size: Dimension, lifeAt: Set[Position]) extends UniverseLike {
      def allPositions = size.allPositions
      protected def updateLifeAt(newLifePositions: Set[Position]): Universe = copy(lifeAt = newLifePositions)
    }

    def universeFrom(size: Dimension, lifeAt: Set[Position]): Universe = Universe(size, lifeAt)

    protected def stayAlive(count: Int): Boolean = count >= 4 && count <= 10
    protected def beReborn(count: Int): Boolean = count >= 8 && count <= 10
  }

  trait ThreeDimensionsUIModule extends UIModule with ThreeDimensionsModule {
    import java.awt.{Graphics, Color}

    private val scale = 10
    private val padding = 20

    protected def sizeOfPanel(size: Dimension) = requiredDrawingArea(size)

    protected def clearPanel(graphics: Graphics, size: Dimension): Unit = { 
      val (width, height) = requiredDrawingArea(size)
      graphics.setColor(Color.BLACK)
      graphics.fillRect(0, 0, width, height)
      drawWireFrameCuboid(size, graphics)
    }

    protected def drawPosition(graphics: Graphics, size: Dimension)(position: Position): Unit =
      drawSolidCuboid(size, position, graphics)

    protected def orderForRendering(positions: Set[Position]): Seq[Position] = {
      def positionOrder(left: Position, right: Position) = {
        val xDiff = right.x - left.x
        val yDiff = right.y - left.y
        val zDiff = left.z - right.z

        if ( zDiff == 0 ) {
          if ( yDiff == 0 ) (xDiff < 0)
          else (yDiff < 0)
        }
        else (zDiff < 0)
      }

      positions.toList sortWith positionOrder
    }

    protected def addFurniture(graphics: Graphics, size: Dimension): Unit =
      drawWireFrameCuboid(size, graphics)

    private val cos30 = Math.cos(Math.toRadians(30.0))
    private val sin30 = Math.sin(Math.toRadians(30.0))

    private def adjacent(l: Int) = Math.round(cos30 * l * scale).toInt
    private def opposite(l: Int) = Math.round(sin30 * l * scale).toInt

    private def requiredDrawingArea(size: Dimension): (Int, Int) =
      (adjacent(size.depth) + adjacent(size.width) + (2 * padding), 
       opposite(size.width) + opposite(size.depth) + (size.height * scale) + (2 * padding))

    private def drawWireFrameCuboid(size: Dimension, graphics: Graphics): Unit =
      wireFrameCuboid(graphics, size, size, Position(0, 0, 0))

    private def drawSolidCuboid(size: Dimension, position: Position, graphics: Graphics): Unit =
      solidCuboid(graphics, size, Dimension(1, 1, 1), position)

    private val wireFrameCuboid = drawPolygon(false, (_, _) => Color.WHITE, (_, _) => Color.GRAY)_
    private val solidCuboid = drawPolygon(true, zAxisRelatedColor, (_, _) => Color.GRAY)_

    private def drawPolygon(filled: Boolean, 
                            visibleColor: (Int, Int) => Color,
                            behindColor: (Int, Int) => Color)
                           (graphics: Graphics, 
                            universeSize: Dimension, 
                            polygonSize: Dimension, 
                            relativePosition: Position) = {
      def padded(p: (Int, Int)) = (p._1 + padding, p._2 + padding)
      def polygon(points: (Int, Int)*): Unit = {
        val p = points.toArray map padded
        val xPositions = p map (_._1)
        val yPositions = p map (_._2)
        if (filled) graphics.fillPolygon(xPositions, yPositions, points.size) 
        else graphics.drawPolygon(xPositions, yPositions, points.size)
      }

      val oppWidth = opposite(polygonSize.width)
      val adjWidth = adjacent(polygonSize.width)
      val scaledHeight = polygonSize.height * scale

      val xOffset = adjacent(relativePosition.x) + adjacent(relativePosition.z)
      val yOffset = opposite(universeSize.width) + (relativePosition.y * scale) + opposite(relativePosition.z) - opposite(relativePosition.x)

      val pointA = (xOffset, yOffset)
      val pointB = (pointA._1 + adjacent(polygonSize.depth), pointA._2 + opposite(polygonSize.depth))
      val pointC = (pointB._1 + adjWidth, pointB._2 - oppWidth)
      val pointD = (pointB._1, pointB._2 + scaledHeight)
      val pointE = (pointA._1 + adjWidth, pointA._2 - oppWidth)
      val pointF = (pointA._1, pointA._2 + scaledHeight)
      val pointG = (pointC._1, pointC._2 + scaledHeight)
      val pointH = (pointE._1, pointE._2 + scaledHeight)

      if (!filled) {
        graphics.setColor(behindColor(universeSize.depth, relativePosition.z))
        polygon(pointA, pointE, pointH, pointF)
        polygon(pointE, pointH, pointG, pointC)
      }

      graphics.setColor(visibleColor(universeSize.depth, relativePosition.z))
      polygon(pointA, pointB, pointD, pointF)
      polygon(pointB, pointC, pointG, pointD)
      polygon(pointA, pointB, pointC, pointE)
    }

    private def zAxisRelatedColor(depth: Int, z: Int): Color = {
      def linearGradientValue(size: Int, position: Int, ascending: Boolean) = {
        val minValue = 0x00
        val maxValue = 0xFF
        val pos = if (ascending) position else (size - position)
        val value = maxValue - ((maxValue / size) * pos)
        if ( value < minValue ) minValue else if ( value > maxValue ) maxValue else value
      }

      def middleWeightedGradientValue(depth: Int, z: Int) = {
        val edgeDepth = 0x00
        val middleDepth = 0xFF
        val shift = (middleDepth - edgeDepth) / (depth / 2) 
        val value = if ( z > (depth / 2) ) middleDepth - (shift * (z - depth / 2)) else edgeDepth + (shift * z)
        if ( value < edgeDepth ) edgeDepth else if ( value > middleDepth ) middleDepth else value
      }

      new Color(linearGradientValue(depth, z, true), 
                middleWeightedGradientValue(depth, z), 
                linearGradientValue(depth, z, false))
    }
  }

  trait ThreeDimensionsConfigLoaderModule extends ConfigLoaderModule with ThreeDimensionsModule { 
    protected def parseUniverse(lines: List[String]): (Dimension, Set[Position]) = {
      val linesByPlane = groupLines(lines)
      val allAlivePositions = for {
        (alivePositions, index) <- (linesByPlane map parsePlane).zipWithIndex
        position <- alivePositions
      } yield Position(position._1, position._2, index)

      (Dimension(linesByPlane.head.head.length, linesByPlane.head.length, linesByPlane.length), allAlivePositions.toSet)
    }

    private def notSlash(s: String) = s != "/"

    @scala.annotation.tailrec
    private def groupLines(lines: List[String], planes: List[List[String]] = Nil): List[List[String]] = lines match {
      case Nil => planes.reverse
      case "/" :: xs => groupLines(xs, planes)
      case xs => groupLines(xs dropWhile notSlash, (xs takeWhile notSlash) :: planes )
    }

    private def parsePlane(lines: List[String]): Seq[(Int, Int)] = {
      def cellWithPosition(acc: Seq[(Char, (Int, Int))], line: (String, Int)): Seq[(Char, (Int, Int))] = {
        val (items, index) = line
        acc ++ (items.zipWithIndex map (item => (item._1 -> (index, item._2))))
      } 

      val cellsWithIndex = lines.zipWithIndex.foldLeft(Seq.empty[(Char, (Int, Int))])(cellWithPosition)
      cellsWithIndex filter (_._1 == 'o') map (_._2.swap)
    }
  }
}

object ThreeDimensionsGameOfLife extends App with GameEngineModule with ThreeDimensionsModule {
  object GameOfLife extends GameLoopModule with ThreeDimensionsRulesModule with ThreeDimensionsUIModule with ThreeDimensionsConfigLoaderModule
  GameOfLife.start(args(0))
}
