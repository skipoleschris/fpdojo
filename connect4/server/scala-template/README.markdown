# Connect4 Scala Template

This is a template projecty for building the Connect4 server in Scala. It is a cut-down version of the reference implementation that provides the following features:

* An SBT build configuration
* A default set of Scala libraries for building and testing a RESTful concurrent server
* A starting code structure that answers a simple ping request
* Skeleton unit tests that excersise the code that handles the ping request

NOTE: This is only a starting skeleton for those who wish to use it. Please feel free to swap to your own favourite libraries, restructure the code to suit your own way of working and so on.

## Building The Code

To build the code you will need the default Scala build tool, which is called SBT. The project has been tested using SBT 0.12.2. (If you are using a Mac and Homebrew then 'brew install sbt' will work just fine.). The code is written using Scala 2.10.0, but SBT will download the correct Scala version for you.

To build the code, go into the root directory (scala-template) and type

    sbt

You will be presented with a command prompt. To compile the code type 'compile', to compile and run the tests type 'test', to enter an interactive Scala console with your application classpath already configured type 'console':

    > console
    [info] Starting scala interpreter...
    [info] 
    Welcome to Scala version 2.10.0 (Java HotSpot(TM) 64-Bit Server VM, Java 1.7.0_17).
    Type in expressions to have them evaluated.
    Type :help for more information.

    scala>

Once at the Scala prompt you can start the web server by issuing the following command:

    com.equalexperts.connect4.Connect4Server.main(Array())

This will start the web server on port 8081 (pass a String parameter in the array to use a different port). The server can be stopped at any time by pressing a key and you can return to SBT by issuing the ':q' command.

Once the server is running, you should be able to go to your browser and enter the following URL to see the credits Json document:

    http://localhost:8081/connect4/ping

Congratulations, you have a running server!

## Libraries Used

The template application uses a number of common Scala libraries to provide most of the underlying framework. Full details of libraries and versions can be found in the project/Build.scala file. These are described below:

### Scala

Uses Scala 2.10.0 for the implementation. It should be possible to write the main game module in pure Scala with no dependencies.

### Akka Actors

The Akka library is used to provide support for actors. These provide a mechanism for handling concurrency concerns, providing one approach to ensuring basic game rules, like only two players being registered for a game and players must take alrernate turns to update the board. 

### Unfiltered

The Unfiltered library provides a super-lightweight web framework that is very good for building RESTful services. It is configured to use the Netty asynchronous HTTP server as its underlying container implementation and the Lift-Json library for Json parsing and generation.

### Specs2 and ScalaCheck

Unit testing support is provided by the Specs2 testing framework. Also included is the ScalaCheck framework that can be used for property based testing.

## Module Structure

The modules are structured using the cake pattern, which is a Scala alternative to dependency injection. The basic principle is that each module is defined in a trait and these traits are then layered together at instantiation time to compose the completed application. The starting module structure provided is as follows:

### Game Module

The implementation of the underlying game logic. The template has it just holding the credits text.

### Players Module

This module manages the lifecycle of games, registration and the concurrency aspects of two players both trying to query and update the game. It has a contract trait and an implementation trait that is based around the Akka Actors model. This separation is needed so that tests for the REST module can stub the players moudle and not need to worry about the asynchronous processing associated with actors.

Currently the template implementation initialises the Akka framework and starts an actor that can handle requests for the credits in an asynchronous manner.

### REST Module

This module contains the Plan (which is roughly the same as a servlet) and the controller implementation that calls the players module and renders the response Json documents. Given that the asynchronous Netty server is being used and the default player module is using asynchronous actors, the controller is also asynchronous as all Json rendering is done in a Future.

### Connect4 Server

This is the main routine that starts the server. It can be started from the interactive Scala console or started as a standalone app as it implements the App trait, which gives is a static main method.

## Testing

The test directory has unit tests for each of the modules.

### Testing the Game Module

These are just simple unit tests on pure Scala code.

### Testing the Players Module

The test for the players module has to deal with the fact that Akka Actors are by default asynchronous. It therefore overrides the factory that creates actors and instead returns single-threaded, deterministic actors instead which can be used for unit testing. Integration testing of the full actor system is left as an excerise for the reader, although the Akka documentation has an excellent tutorial on how to achieve it.

### Testing the REST Module

The REST module testing is split into two parts. 

The tests for the Controller part implement stubs for the players module and invoke the controller directly and assert against the Http response code and the Json content returned in the response body (but without making actual Http requests - it uses stub objects).

The tests for the Plan part stub out the controller to always return Ok for each request. A Netty server is then fired up and each URL is called using an Http client. The Netty server is stopped when the test is complete.





