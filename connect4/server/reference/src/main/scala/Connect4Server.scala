package com.equalexperts.connect4

import unfiltered.netty._

object Connect4Server extends App {
  
  val gameServer = new RestModule with ActorBasedPlayersModule
  val port = args.headOption map (_.toInt) getOrElse 8081

  val http = Http(port).plan(gameServer.Connect4Plan).start()
  
  println("Server listening on port: %d. Press any key to exit...".format(port))
  System.in.read()

  http.stop()
  gameServer.shutdown()
}
