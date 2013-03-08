rm classes/*
scalac -d classes *.scala
scala -cp classes TwoDimensionsGameOfLife 2dLife.txt

