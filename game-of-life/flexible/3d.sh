rm classes/*
scalac -d classes *.scala
scala -cp classes ThreeDimensionsGameOfLife 3dLife.txt

