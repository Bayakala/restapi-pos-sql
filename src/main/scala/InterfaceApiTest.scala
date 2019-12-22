package com.datatech.rest.sql
import akka.actor._
import com.datatech.rest.sql.InterfaceApis._
import scala.util._
import scala.concurrent._
import scala.concurrent.duration._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import PosModel._

object InterfaceApiTest extends App with JsonConverter {
  implicit val system = ActorSystem()
  implicit val ec = system.dispatcher
  val app_id = "wxf09f0fb645086922"
  val app_secret = "48119de96ee2f0d023203e2e9650129d"
  val app_url = "http://10.200.5.50:8081/api/Auth/access_token"
  val app_key = "a4da0ac8ff6aa7f116fbf2f67c424a06"

  val futJwt: Future[String] = getJwt(app_url,app_id,app_secret,app_key)
  /*
  futJwt.onComplete {
    case Success(jstr) => println(jstr)
    case Failure(err) => println(s"err:${err.getMessage}")
  } */
  val jwt = Await.result(futJwt,10.seconds)
  println(jwt)
  scala.io.StdIn.readLine()

  val member_url = "http://10.200.5.50:8082/api/MemberApi/GetMember"
  var member_params = Map[String,String](
    "phone" -> "20000000001",
   // "card_no" -> "123456",
    "app_secret" -> app_secret,
    "nonce_str" -> "234",
    "store_no" -> "1101"
    )
  val authorization = headers.Authorization(OAuth2BearerToken(jwt))
  val memberInfoRequest = HttpRequest(
    HttpMethods.POST,
    uri = member_url,
  ).addHeader(headers.Authorization(OAuth2BearerToken(jwt)))
  val futMemberInfo = getResponseJson(memberInfoRequest,member_params,jwt,app_id,app_key)
  futMemberInfo.onComplete {
    case Success(ojstr) =>
      ojstr match {
        case Some(jstr) =>
          println(s"return: ${fromJson[List[MemberData]](jstr)}")
        case _ =>
      }
    case Failure(err) => println(s"err:${err.getMessage}")
  }
  scala.io.StdIn.readLine()

  system.terminate()
}
