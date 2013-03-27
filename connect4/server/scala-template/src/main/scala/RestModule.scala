package com.equalexperts.connect4

import unfiltered.Async
import unfiltered.request._
import unfiltered.response._
import unfiltered.netty._

trait RestModule extends PlayersModule {

  object Connect4Plan extends async.Plan with ServerErrorResponse {
    def intent = {
      case request @ Path(Seg("connect4" :: "ping" :: Nil)) =>
        request match {
          case GET(_) => controller ping request
          case _ => controller notAllowed request
        }        
    }
  }

  def shutdown() = endGames()

  protected trait Controller {
    def ping[A](req: Async.Responder[A]): Unit
    def notAllowed[A](req: Async.Responder[A]): Unit
  }

  protected def controller: Controller = DefaultController

  protected object DefaultController extends Controller {
    import scala.language.implicitConversions

    private implicit def responderToResponseHandler[A](req: Async.Responder[A]) = new JsonResponseHandler(req)
    
    def ping[A](req: Async.Responder[A]) = showCredits(req)

    def notAllowed[A](req: Async.Responder[A]) = req respond MethodNotAllowed
  }

  private class JsonResponseHandler[A](req: Async.Responder[A]) extends RespondHandler {
    import scala.concurrent.Future
    import scala.concurrent.ExecutionContext.Implicits._
    import net.liftweb.json._
    import JsonDSL._

    def !(response: GameResponse): Unit = Future {
      response match {
        case CreditsResponse(credits) => sendCredits(credits)
      }
    }

    private def sendCredits(credits: String) = 
      req respond (Ok ~> Json(("credits" -> credits)))
  }
}
