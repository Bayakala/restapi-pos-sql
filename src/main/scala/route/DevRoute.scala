package com.datatech.rest.sql
import akka.http.scaladsl.server.Directives
import akka.stream._
import akka.actor.ActorSystem
import akka.http.scaladsl.common._
import PosRepo._
import PosModel._
import AuthBase._
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.jackson

trait JsonCodec extends Json4sSupport {
  import org.json4s.DefaultFormats
  import org.json4s.ext.JodaTimeSerializers
  implicit val serilizer = jackson.Serialization
  implicit val formats = DefaultFormats ++ JodaTimeSerializers.all
}

object DevRoute extends JsonCodec {
  implicit val jsonStreamingSupport = EntityStreamingSupport.json()
    .withParallelMarshalling(parallelism = 8, unordered = false)

  class DevRoute(val pathName: String, val jwt: String)(
    implicit  sys: ActorSystem, mat: Materializer, auth: AuthBase, posRepo: PosRepo) extends Directives with JsonConverter {
    val rsc = new RSConverter

    val route = pathPrefix(pathName) {
      pathPrefix("items") {
        get {
          val (shopid,_,_,_,_) = auth.shopIdFromJwt(jwt)
          val sql = "select dpt, name from items where shopid='"+shopid+"'"
          val rows = posRepo.query[Item]("mpos", sql,  toItem)
    /*      val futRows = rows.map(m => toJson(m)).runFold(Vector[String]()){
            case (v,s) => v :+ s
          } */
          complete(rows)
        }
      } ~
        pathPrefix("paymethods") {
          get {
            val (shopid,_,_,_,_) = auth.shopIdFromJwt(jwt)
            val sql = "select acct, name from paymethods where shopid='"+shopid+"'"
            val rows = posRepo.query[PayMethod]("mpos", sql, toPayMethod)
          /*  val futRows = rows.map(m => toJson(m)).runFold(Vector[String]()){
              case (v,s) => v :+ s
            }*/
            complete(rows)
          }
        }
    }
  }

}
