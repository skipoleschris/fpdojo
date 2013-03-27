# Connect4

The aim of this dojo excercise is to build a two-player server for the game Connect4 (http://en.wikipedia.org/wiki/Connect_Four) and then to implement bots that can compete against each other usind then server.

## Server

There is currently a reference server implementation. This can be used as a source of ideas and inspiration and as the contract for the REST api. The reference server is in the server/reference directory. It can be build using SBT (0.12.x). The server can also be started from sbt by issuing the console command and then typing:

    com.equalexperts.connect4.Connect4Server.main(Array())

This will start the server on port 8081. You can optionally pass a string parameter in the Array to use a different port.

### REST api

The REST api supports the following behaviour:

#### Ping

Check the server is working:

    GET: /connect4/ping

Returns some credits in a Json document.

#### Create Game

A new game must be created by one of the players.

    POST: /connect4/game/create

Starts a new game running on the server. Returns the id of the game to use in subsequent requests. The response Json is:

    {
        "game": {
            "id": 1
    	  }
    }

#### Register Player

Each player must register with the game in order to be able to place pieces. Each game supports exactly two registration requests after which time any registration attempts will return an error. The first player to register is always red and gets to go first.

    POST: /connect4/game/:gameId/register

The playerId that is returned must be used in subsequent requests by that player. The response Json is:

    {
        "registration": {
            "playerId": "fbac9c23-6a15-47c5-8bd6-54d0074a41ff",
            "colour": "red"
        }
    }

#### Status Check

The server is implemented using a polling model. Each player must poll for the status. The returned status contains the current state of the board, whether it is their turn or not an whether there is a winnner.

    GET: /connect4/game/:gameId/player/:playerId/status

The response Json is:

    {
        "status": {
            "grid": [
                ".......",
                ".......",
                ".......",
                ".......",
                ".......",
                "...R..."
            ],
            "ready": true,
            "winner": ""
        }
    }

The grid is a visual representation of the Connect4 board. Periods indicate empty positions, R a red piece and Y a yellow piece. If the ready value is true then the player can place a piece, if false they must wait for the other player and poll for status periodically. If the game has been won then the winner will contain the colour that won and both players will receive a false value for ready. If the game was drawn then the grid will contain no periods and both players will receive a false value for ready.

#### Place Piece

When a player gets the ready status then they can place a piece into one of the columns. The acceptabe column value is between 1 and 7 (from left to right).

    POST: /connect4/game/:gameId/player/:playerId/placepiece/:column

The response Json is:

    {
        "result": {
            "grid": [
                ".......",
                ".......",
                ".......",
                ".......",
                "...Y...",
                "...R..."
            ],
            "winningMove": false
        }
    }

The grid is a visual representation of the Connect4 board with the new piece added. The winningMove value will be set to true if the placing of the piece resulted in the player having won the game.

### Reference Implementation

The server reference implementation is made of four components:

#### GameModule

Implements the underlying game. Creates an event sourced model that captures the placement of the individual pieces into the board. Also provides a grid representation which is created by processing the captured events into a data structure.

#### PlayersModule

Implements the registration and the ability for players to alternatively place pieces and check the game status. The default implementation uses Akka actors to process events and manage any concurrency concerns.

#### RestModule

Implements the RESTful interface. Uses asynchronous processing and futures for generating the responses so as not to block the players module.

#### Connect4Server

Starts an in-memory Netty server and configures it with the REST module and Akka actor based players module. Server can be stopped at any time by pressing a key.





