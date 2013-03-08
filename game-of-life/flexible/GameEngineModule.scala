trait GameEngineModule {

  trait DimensionsModule {
    type Dimension <: DimensionLike
    type Position <: PositionLike

    trait DimensionLike { this: Dimension =>
      def allPositions: Seq[Position] 
    }

    trait PositionLike { this: Position =>
      def adjacentTo(size: Dimension, cells: Set[Position]): Int = 
        cells count adjacent(size)

      protected def adjacent(size: Dimension)(p: Position): Boolean
    }
  }


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


  trait UIModule extends RulesModule {
    import javax.swing._
    import java.awt.{Toolkit, Graphics, Dimension => AwtDimension}
    import java.awt.event.{WindowEvent, WindowAdapter}


    object ui {
      private val frame = new JFrame("Game of Life")

      def displayWindow(size: Dimension): Unit = {
        frame addWindowListener (new WindowAdapter() {
          override def windowClosing(e: WindowEvent): Unit = {
            frame.setVisible(false)
            System.exit(0)
          }
        })
        frame setVisible true
        frame createBufferStrategy 2

        val insets = frame.getInsets
        val (width, height) = sizeOfPanel(size)
        frame setSize (new AwtDimension(width + insets.left + insets.right, height + insets.top + insets.bottom))
      }

      def render(universe: Universe): Unit = {
        val (width, height) = sizeOfPanel(universe.size)
        val bufferStrategy = frame.getBufferStrategy
        val graphics = bufferStrategy.getDrawGraphics

        clearPanel(graphics, universe.size)
        orderForRendering(universe.lifeAt) foreach drawPosition(graphics, universe.size)
        addFurniture(graphics, universe.size)

        graphics.dispose()
        bufferStrategy.show()
        Toolkit.getDefaultToolkit.sync()
      }
    }

    protected def sizeOfPanel(size: Dimension): (Int, Int)
    protected def clearPanel(graphics: Graphics, size: Dimension): Unit
    protected def drawPosition(graphics: Graphics, size: Dimension)(position: Position): Unit
    protected def orderForRendering(positions: Set[Position]): Seq[Position]
    protected def addFurniture(graphics: Graphics, size: Dimension): Unit
  }

  trait ConfigLoaderModule extends RulesModule {
    object loader {
      import scala.io.Source

      def load(filename: String): Universe = {
        def cellWithPosition(acc: Seq[(Char, (Int, Int))], line: (String, Int)): Seq[(Char, (Int, Int))] = {
          val (items, index) = line
          acc ++ (items.zipWithIndex map (item => (item._1 -> (index, item._2))))
        } 

        val lines = (Source.fromFile(filename).getLines filterNot (line => line.startsWith("!") || line.trim.isEmpty)).toList
        val (size, lifeAt) = parseUniverse(lines)
        universeFrom(size, lifeAt)
      }
    }

    protected def parseUniverse(lines: List[String]): (Dimension, Set[Position])
  }

  trait GameLoopModule extends RulesModule with UIModule with ConfigLoaderModule {
    def start(seedFilename: String) = {
      val startingUniverse = loader load seedFilename
      ui.displayWindow(startingUniverse.size)
      loop(startingUniverse)
    }

    @scala.annotation.tailrec
    private def loop(u: Universe): Unit = {
      ui.render(u)
      Thread.sleep(200L)
      loop(u.tick)
    }
  }
}


