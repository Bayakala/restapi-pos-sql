package com.datatech.rest.sql
import MD5Utils._
import DESUtils._
import java.util.{Calendar, TimeZone}

import scala.concurrent._
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.Http
import akka.util.ByteString

import akka.http.scaladsl.util.FastFuture

object InterfaceApis extends JsonConverter {
  import scala.collection.immutable._
  private val TICKS_AT_EPOCH = 621355968000000000L
  private val TICKS_PER_MILLISECOND = 10000
  case class JWT(
                  access_token: String,
                  expires_in: BigInt,
                  token_type: String
                )
  case class RESPONSE(code: String,info: String, data: String)
  def jwtFromMap: Map[String,Any] => JWT = m => JWT(
    access_token = m("access_token").toString,
    expires_in = m("expires_in").asInstanceOf[BigInt],
    token_type = m("token_type").toString
  )
  def getJwt(issuerUri: String, app_id: String, app_secret: String, app_key: String
            )(implicit sys: ActorSystem):Future[String] = {
    import sys.dispatcher
    val authRequest = HttpRequest(
      HttpMethods.POST,
      uri = issuerUri,
      entity = HttpEntity(ContentTypes.`application/json`,
            ByteString(jwtReqParams(app_id,app_secret,app_key).toString))
    )
    val futJwt = (for {
      resp <- Http().singleRequest(authRequest)
      payload <- resp.entity.dataBytes.runFold("") {(s,b) => s + b.utf8String}
      respData <- FastFuture.successful(fromJson[Map[String,Any]](payload))
      jwt <- FastFuture.successful(jwtFromMap(respData("data").asInstanceOf[Map[String,Any]]))
    } yield jwt).map(jwt => jwt.access_token)
    futJwt
  }
  def jwtReqParams(app_id: String, app_secret: String, app_key: String): String = {
    var mapParams = Map[String,String](
      "app_id" -> app_id,
      "app_secret" -> app_secret,
      "nonce_str" -> "123",
      )

    val json = signData(mapParams,app_key)
    val signedId = md5(app_id+ "&key=" + app_id)
    val encryptKey = md5(signedId).toUpperCase.substring(0,8)
    val cryptedData = encrypt(json,encryptKey).toUpperCase
    val jwt = "{\"data\":\""+cryptedData+"\",\"app_id\":\""+app_id+"\"}"
    jwt
  }

  def signData(params: Map[String,Any], key: String): String = {
    val utc = TimeZone.getTimeZone("UTC")
    val stamp = Calendar.getInstance(utc).getTimeInMillis
    val mapDscByKey = ListMap((params + ("time_stamp" -> stamp.toString())).toSeq.sortWith(_._1 < _._1):_*)
    val strParams = mapDscByKey.foldLeft("") {
      case (s,p) =>  if(s.length == 0)
        p._1+"="+p._2
      else
        s + "&"+p._1+"="+p._2
    }
    val strToSign = strParams + "&key=" + key
    val signiture = md5(strToSign).toUpperCase
    val orderedParams = ListMap(mapDscByKey.toSeq.sortWith(_._1 < _._1):_*)
    toJson(orderedParams + ("sign" -> signiture))
  }

  def getResponseJson(reqWithJwt: HttpRequest, params: Map[String,Any],jwt: String, app_id: String, app_key: String)(
  implicit sys: ActorSystem): Future[Option[String]] = {
    import sys.dispatcher
    val signedJson = signData(params,app_key)
    val encryptKey = md5(md5(jwt+ "&key=" + app_id)).toUpperCase.substring(0,8)
    val cryptedData = encrypt(signedJson,encryptKey)
    val json = "{\"data\":\""+cryptedData+"\"}"
    val futJson: Future[Option[String]] = for {
      resp <- Http().singleRequest(reqWithJwt.copy(entity =
        HttpEntity(ContentTypes.`application/json`,ByteString(json))))
      data <- resp.entity.dataBytes.runFold("") {(s,b) => s + b.utf8String}
      respData <- FastFuture.successful(fromJson[RESPONSE](data))
      json <- FastFuture.successful(getJson(respData,jwt,app_key))
    } yield json
    futJson
  }
  def getJson(resp: RESPONSE, jwt: String, app_key: String): Option[String] = {
    if (resp.code == "200" && resp.data.trim != "") {
      val md51=md5(jwt+ "&key=" + app_key).toUpperCase;
      val encryptKey = md5(md51).substring(0,8).toUpperCase
      Some(decrypt(resp.data,encryptKey))
    }
    else
      None
  }
}
