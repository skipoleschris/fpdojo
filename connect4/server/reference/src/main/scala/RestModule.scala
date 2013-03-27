package com.equalexperts.connect4

import unfiltered.Async
import unfiltered.request._
import unfiltered.response._
import unfiltered.netty._

trait RestModule extends PlayersModule {

  object Connect4Plan extends async.Plan with ServerErrorResponse {
    def intent = {
      case request @ Path(Seg("connect4" :: "ping" :: Nil)) =>
        get(request, controller.pong)
      case request @ Path(Seg("connect4" :: "game" :: "create" :: Nil)) =>
        post(request, controller.handleCreateGame)
      case request @ Path(Seg("connect4" :: "game" :: id :: "register" :: Nil)) =>
        post(request, controller.handleRegisterPlayer(id))
      case request @ Path(Seg("connect4" :: "game" :: id :: "player" :: playerId :: "status" :: Nil)) =>
        get(request, controller.handleCheckStatus(id, playerId))
      case request @ Path(Seg("connect4" :: "game" :: id :: "player" :: playerId :: "placepiece" :: column :: Nil)) =>
        post(request, controller.handlePlacePiece(id, playerId, column))
    }

    private def get[A](request: Async.Responder[A], f: (Async.Responder[A] => Unit)) = request match {
      case GET(_) => f(request)
      case _ => controller notAllowed request
    }

    private def post[A](request: Async.Responder[A], f: (Async.Responder[A] => Unit)) = request match {
      case POST(_) => f(request)
      case _ => controller notAllowed request
    }
  }

  def shutdown() = endGames()

  private object controller {
    import scala.language.implicitConversions
    import scala.util.Try

    implicit def responderToResponseHandler[A](req: Async.Responder[A]) = new JsonResponseHandler(req)
    
    def pong[A](req: Async.Responder[A]) = req respond (Ok ~> ResponseString("pong"))

    def notAllowed[A](req: Async.Responder[A]) = req respond MethodNotAllowed

    def handleCreateGame[A](req: Async.Responder[A]) = startGame(req)

    def handleRegisterPlayer[A](gameId: String)(req: Async.Responder[A]) = {
      registerPlayer(parseGameId(gameId), req)
    }

    def handleCheckStatus[A](gameId: String, playerId: String)(req: Async.Responder[A]) = {
      checkStatus(parseGameId(gameId), playerId, req)
    }

    def handlePlacePiece[A](gameId: String, playerId: String, column: String)(req: Async.Responder[A]) = {
      placePiece(parseGameId(gameId), playerId, parseColumn(column), req)
    }

    private def parseGameId(gameId: String) = Try(gameId.toLong) getOrElse 0L
    private def parseColumn(column: String) = Try(column.toInt) getOrElse 0
  }

  private class JsonResponseHandler[A](req: Async.Responder[A]) extends RespondHandler {
    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits._
    import net.liftweb.json._
    import JsonDSL._

    def !(response: GameResponse): Unit = Future {
      response match {
        case ErrorResponse(code) => sendError(code)
        case StartedResponse(id) => sendStarted(id)
        case RegisteredResponse(playerId, colour) => sendRegistered(playerId, colour)
        case CheckResponse(grid, ready, winner) => sendCheckResult(grid, ready, winner)
        case PlaceResponse(grid, winningMove) => sendPlaceResult(grid, winningMove)
      }
    }

    private def sendError(code: ErrorCode) = 
      req respond (BadRequest ~> Json(("error" -> ("code" -> code.toString))))

    private def sendStarted(id: Long) =
      req respond (Ok ~> Json(("game" -> ("id" -> id))))

    private def sendRegistered(playerId: String, colour: Colour) = 
      req respond (Ok ~> Json(("registration" -> (
        ("playerId" -> playerId) ~ 
        ("colour" -> colour.describe)
      ))))

    private def sendCheckResult(gridView: Grid, ready: Boolean, winner: Option[Colour]) =
      req respond (Ok ~> Json(("status" -> (
        ("grid" -> grid.asStringRepresentation(gridView)) ~
        ("ready" -> ready) ~
        ("winner" -> (winner map (_.describe) getOrElse ""))
      ))))

    private def sendPlaceResult(gridView: Grid, winningMove: Boolean) =
      req respond (Ok ~> Json(("result" -> (
        ("grid" -> grid.asStringRepresentation(gridView)) ~
        ("winningMove" -> winningMove)
      ))))  
  }
}
