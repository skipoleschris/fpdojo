
product' :: (Num a) => a -> a -> a
product' x y = x * y


getNumberFromUser :: IO(Int)
getNumberFromUser = do
  putStrLn("Enter an integer:")
  line <- getLine
  return (read line :: Int)

showResult :: String -> Int -> IO()
showResult operation result = 
  putStrLn ("The " ++ operation ++ " is: " ++ show result)

main :: IO()
main = do
  x <- getNumberFromUser
  y <- getNumberFromUser
  let result = product' x y 
  showResult "product" result

