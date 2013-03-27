package com.equalexperts.connect4

import unfiltered.response.HttpResponse
import unfiltered.Cookie
import unfiltered.Async
import unfiltered.response.ResponseFunction

import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch

class TestResponder extends Async.Responder[Any] { 
  val response = new TestHttpResponse()
  val latch = new CountDownLatch(1)
  def respond(rf: ResponseFunction[Any]): Unit = {
    rf(response)
    latch.countDown()
  }
}

class TestHttpResponse extends HttpResponse[Any] {
  private var sentStatus: Option[Int] = None
  private var redirectTo: Option[String] = None

  private val output = new ByteArrayOutputStream()
  private val sentHeaders = scala.collection.mutable.Map[String, String]()
  private val sentCookies = scala.collection.mutable.Seq[Cookie]()

  // Response methods
  def status(statusCode: Int): Unit = sentStatus = Some(statusCode)
  def outputStream = output
  def redirect(url: String): Unit = redirectTo = Some(url)
  def header(name: String, value: String): Unit = sentHeaders.put(name, value)
  def cookies(cookie: Seq[Cookie]): Unit = sentCookies ++ cookie

  // Query sent response
  def status = sentStatus.getOrElse(0)
  def redirect = redirectTo
  def body = output.toString
  def header(name: String) = sentHeaders(name)
  def headerNames = sentHeaders.keysIterator
  def cookies = sentCookies
}
