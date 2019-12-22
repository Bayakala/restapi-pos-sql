package com.datatech.rest.sql
import akka.http.scaladsl.server.Directives
import akka.stream._
import akka.actor.ActorSystem
import akka.http.scaladsl.common._
import PosRepo._
import AuthBase._
import akka.http.scaladsl.model.{HttpMethods, StatusCodes, Uri}
import akka.http.scaladsl.util.FastFuture
import akka.http.caching.scaladsl.Cache
import akka.http.scaladsl.server.RequestContext
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.server.directives.CachingDirectives._

import scala.util._

object PosRoute {
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()
    .withParallelMarshalling(parallelism = 8, unordered = false)

  class PosRoute(val pathName: String, val jwt: String, gcache: Cache[Uri,RouteResult])(
    implicit  sys: ActorSystem, mat: Materializer, auth: AuthBase, posRepo: PosRepo) extends Directives with JsonConverter {

    import sys.dispatcher

    //Example keyer for non-authenticated GET requests
    val simpleKeyer: PartialFunction[RequestContext, Uri] = {
      val isGet: RequestContext => Boolean = _.request.method == HttpMethods.GET
      //      val isAuthorized: RequestContext => Boolean =
      //        _.request.headers.exists(_.is(Authorization.lowercaseName))
      val result: PartialFunction[RequestContext, Uri] = {
        case r: RequestContext if isGet(r) => r.request.uri
      }
      result
    }
    val route = pathPrefix(pathName) {
      pathPrefix("getuserinfo") {
        (get & parameter('userid)) { userid => {
          alwaysCache(gcache, simpleKeyer) {
            onComplete(posRepo.futureUserInfo(userid)) {
              _ match {
                case Success(oui) => oui match {
                  case Some(ui) => complete(toJson(ui))
                  case None => complete(toJson(Map[String, Any](("TERMINALID" -> ""))))
                }
                case Failure(_) => complete(toJson(Map[String, Any](("TERMINALID" -> ""))))
              }
            }
          }
        }
        }
      } ~
        pathPrefix("export") {
          (post & parameter('tbl)) { tbl => {
            entity(as[String]) { json =>
              complete(FastFuture.successful(takeInTxns(jwt, json, tbl)).map(_ => StatusCodes.OK))
            }
          }
          }
        }
    }
  }

}
