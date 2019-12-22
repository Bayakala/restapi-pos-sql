package com.datatech.rest.sql
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import pdi.jwt._
import AuthBase._
import PosRepo._
import com.datatech.sdp.jdbc.config.ConfigDBsWithEnv
import akka.http.scaladsl.server.RouteResult
import akka.http.scaladsl.model.Uri
import akka.actor.ActorSystem
import akka.http.caching.scaladsl.{Cache, CachingSettings}
import akka.http.caching.LfuCache
import scala.concurrent.duration._

import Repo._
import SqlRoute._
import PosRoute._
import DevRoute._

object POSHttpServer  {

  def main(args: Array[String]) {

    val hostPat = "(.*):(.*)".r
    val (host, port) = args(0) match {
      case hostPat(h, p) => (h, p)
      case _ => ("localhost", "50001")
    }

    implicit val httpSys = ActorSystem("pos-http-sys")
    //  implicit val httpMat = ActorMaterializer()
    implicit val httpEC = httpSys.dispatcher

    ConfigDBsWithEnv("prod").setup('mpos)
    ConfigDBsWithEnv("prod").loadGlobalSettings()

    implicit val repo = new PosRepo

    implicit val authenticator = new AuthBase()
      .withAlgorithm(JwtAlgorithm.HS256)
      .withSecretKey("OpenSesame")
      .withUserFunc(repo.getValidUser)

    val defaultCachingSettings = CachingSettings(httpSys)
    val lfuCacheSettings =       //最少使用排除算法缓存
      defaultCachingSettings.lfuCacheSettings
        .withInitialCapacity(128)  //起始单位
        .withMaxCapacity(1024)   //最大单位
        .withTimeToLive(1.hour)         //最长存留时间
        .withTimeToIdle(30.minutes)     //最长未使用时间
    val cachingSettings =
      defaultCachingSettings.withLfuCacheSettings(lfuCacheSettings)

    //Uri->key, RouteResult -> value
    val lfuCache = LfuCache[Uri,RouteResult](cachingSettings)
    val route =
      path("auth") {
        authenticateBasic(realm = "auth", authenticator.getUserInfo) { userinfo =>
          post {
            complete(authenticator.issueJwt(userinfo))
          }
        }
      } ~
        pathPrefix("api") {
          authenticateOAuth2(realm = "api", authenticator.authenticateToken) { token =>
            new SqlRoute("sql", token)(new JDBCRepo)
              .route ~
              new PosRoute("pos", token,lfuCache)
                .route
          }
        } ~
   //    pathPrefix("posapi") {
          authenticateOAuth2(realm = "posapi", authenticator.authenticateToken) { token =>
            new DevRoute("posapi", token)
              .route
            // ~ ...
          }
    //    }


    val bindingFuture = Http().bindAndHandle(route, host, port.toInt)

    println(s"Server running at $host $port. Press any key to exit ...")

    scala.io.StdIn.readLine()

    bindingFuture.flatMap(_.unbind())
      .onComplete(_ => httpSys.terminate())
  }

}