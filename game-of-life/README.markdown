# Conway's Game of Life

This is an example Scala implementation of [Conway's Game of Life](http://en.wikipedia.org/wiki/Conway's_Game_of_Life). It was the subject of the first Functional Programming Dojo.

The final implementation is the file GameOfLife.scala and is a standalone Scala script that can be run from the command line:

	> scala GameOfLife.scala

The game reads the file life.txt and uses this to create the initial universe. It then opens a window and draws multiple repetitions of the universe until it becomes stable. This version supports wrapping of the game world. Press Crtl+C to exit. The format of the life.txt file is:

	! Comment lines start with an exclamation mark
	! All other lines represent the initial universe
	! All lines of the universe must be the same length
	! A period represents a non-alive point and a lower-case o represents life
	..........
	......o...
	.......o..
	.....ooo..
	..........
	..........
	..........
	.oooo.....
	...o......
	..ooo.....

Also included in the directory is a file called InitialVersion.scala. This is the first draft of the solution that was then refactored into the final game. This is useful as it shows some of the evolution in the thinking of the logic. In particular you can see how the solution evolved from holding a full gird of Boolean values to just holding the positions that were alive. You can also see all of the unecessary for comprehensions and creation of thousands of Position tuples that was eliminated in the improved design. The initial version also does not include seeding from the text file (you have to manually build the initial universe).

