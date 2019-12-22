import java.io.InputStream

import akka.actor._
import akka.http.scaladsl.model.headers._

import scala.concurrent._
import scala.concurrent.duration._
import akka.stream.ActorMaterializer

import akka.http.scaladsl.marshalling.Marshal

import com.datatech.rest.sql._

import akka.http.scaladsl.Http
import akka.util.ByteString
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import akka.http.scaladsl.common.EntityStreamingSupport

import akka.http.scaladsl.model._

import com.datatech.sdp.jdbc.engine.JDBCEngine
import com.github.tasubo.jurl.URLEncode
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.unmarshalling._
import akka.http.scaladsl.marshalling.Marshal


trait JsFormats2 extends SprayJsonSupport with DefaultJsonProtocol

case class Item(dpt: String, name: String)
case class PayMethod(acct: String, name: String)

object TestCrudClient extends JsFormats2 with JsonConverter {
  case class A(code: String, name: String)
  class Person(code: String, name: Option[String], age: Int, male: Boolean
              ,dofb: java.time.LocalDate, pic: Option[InputStream])
  type UserInfo = Map[String,Any]
  def main(args: Array[String]): Unit = {

    val js = toJson(Item("01","book"))
    val a = new A("0001","tiger chan")
    val p = new Person("1010",Some("tiger"),29,true
    ,JDBCEngine.jdbcSetDate(1962,5,1),None)


    implicit val system = ActorSystem()
   // implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    implicit val jsonStreamingSupport = EntityStreamingSupport.json()
      .withParallelMarshalling(parallelism = 8, unordered = false)
    import scala.util._


    def map2Item: Map[String,Any] => Item = m => Item(
      dpt = m("dpt").toString,
      name = m("name").toString
    )
    def update(url: String, cmds: Seq[String])(implicit token: Authorization): Future[HttpResponse] =
    for {
      reqEntity <- Marshal(cmds).to[RequestEntity]
      response <- Http().singleRequest(HttpRequest(
        method=HttpMethods.PUT,uri=url,entity=reqEntity)
      .addHeader(token))
    } yield response

    val authorization = headers.Authorization(BasicHttpCredentials("POCSERVER", "123456"))
    val authRequest = HttpRequest(
      HttpMethods.POST,
      uri = "http://192.168.1.189:50001/auth",
      headers = List(authorization)
    )

    val futToken: Future[HttpResponse] = Http().singleRequest(authRequest)
    val respToken = for {
      resp <- futToken
      jstr <- resp.entity.dataBytes.runFold("") {(s,b) => s + b.utf8String}
    } yield jstr
    val jstr =  Await.result[String](respToken,20 seconds)
    println(jstr)

    implicit val authentication = headers.Authorization(OAuth2BearerToken(jstr))
    scala.io.StdIn.readLine()


    val getUserInfoRequest = HttpRequest(
      HttpMethods.GET,
      uri = "http://192.168.1.189:50001/api/pos/getuserinfo?userid=johnny"
    ).addHeader(authentication)

    val futUI = (for {
      resp <- Http().singleRequest(getUserInfoRequest)
      ui <- Unmarshal(resp.entity).to[String]
    } yield ui).map(fromJson[Map[String,Any]])
    val ui = Await.result(futUI,20 seconds)
    println(ui)

    scala.io.StdIn.readLine()
/*
    val futUI2 = (for {
      resp <- Http().singleRequest(getUserInfoRequest)
      ui <- Unmarshal(resp.entity).to[String]
    } yield ui).map(fromJson[Map[String,Any]])
    val ui2 = Await.result(futUI,20 seconds)
    println(ui2)

    scala.io.StdIn.readLine()

    val futUI3 = (for {
      resp <- Http().singleRequest(getUserInfoRequest)
      ui <- Unmarshal(resp.entity).to[String]
    } yield ui).map(fromJson[Map[String,Any]])
    val ui3 = Await.result(futUI,20 seconds)
    println(ui3)

    scala.io.StdIn.readLine()

    val get1001Request = HttpRequest(
      HttpMethods.GET,
      uri = "http://192.168.1.189:50001/api/pos/getuserinfo?userid=1001"
    ).addHeader(authentication)
    val futUI1001 = (for {
      resp <- Http().singleRequest(get1001Request)
      ui <- Unmarshal(resp.entity).to[String]
    } yield ui).map(fromJson[Map[String,Any]])
    val ui1001 = Await.result(futUI1001,20 seconds)
    println(ui1001)

    scala.io.StdIn.readLine()
    */
    val getItemsRequest = HttpRequest(
      HttpMethods.GET,
      uri = "http://192.168.1.189:50001/posapi/items"
    ).addHeader(authentication)

    val futItems = (for {
      resp <- Http().singleRequest(getItemsRequest)
      items <- Unmarshal(resp.entity).to[List[String]]
    } yield items)  //.map(s => fromJson[Item](s))
    val lstStr = Await.result(futItems,20 seconds)
    println(lstStr)
    val items = lstStr.map(fromJson[Item])
    println(items)
//    println(items)
    scala.io.StdIn.readLine()

    val getPayRequest = HttpRequest(
      HttpMethods.GET,
      uri = "http://192.168.1.189:50001/posapi/paymethods"
    ).addHeader(authentication)

    val futPay = (for {
      resp <- Http().singleRequest(getPayRequest)
      items <- Unmarshal(resp.entity).to[List[String]]
    } yield items.map(fromJson[PayMethod]))
    val lstP = Await.result(futPay,20 seconds)
    println(lstP)
//    val paymethods = lstP.map(fromJson[PayMethod])
//    println(paymethods)
    scala.io.StdIn.readLine()

/*
    import com.datatech.rest.sql.PosModel._
    val data: List[RawTxns] = List(
      RawTxns("20190728","10:35:18","999",2,1,6,13,1,0,0,0,0,"","999","",""),
      RawTxns("20190728","10:35:18","999",2,2,0,2,1,1000,1000,0,0,"","911208763","002","01"),
      RawTxns("20190728","10:35:18","999",2,3,0,2,1,1000,1000,0,0,"","911208763","002","01")
    )
    val txfRequest = HttpRequest(
      HttpMethods.POST,
      uri = "http://192.168.0.189:50001/api/pos/export",
    ).addHeader(authentication)
    val js = toJson(data).toString
    (for {
 //     reqEntity <- Marshal(data).to[RequestEntity]
      respInsert <- Http().singleRequest(txfRequest.copy(entity =
        HttpEntity(ContentTypes.`application/json`,ByteString(toJson(data).toString))))
    } yield respInsert).onComplete {
      case Success(r@HttpResponse(StatusCodes.OK, _, entity, _)) =>
        println("builk insert successful!")
      case Success(_) => println("builk insert failed!")
      case Failure(err) => println(s"Error: ${err.getMessage}")
    }
    scala.io.StdIn.readLine()



    val getAllRequest = HttpRequest(
      HttpMethods.GET,
      uri = "http://192.168.0.189:50081/api/sql/h2/members?sqltext=select%20*%20from%20members"
//      uri = "http://192.168.11.189:50081/api/sql/termtxns/brand?sqltext=SELECT%20*%20FROM%20BRAND",
    ).addHeader(authentication)
    val postRequest = HttpRequest(
      HttpMethods.POST,
      uri = "http://192.168.11.189:50081/api/sql/crmdb/brand?sqltext=insert%20into%20simplebrand(code,name)%20values(?,?)",
    ).addHeader(authentication)

    (for {
      response <- Http().singleRequest(getAllRequest)
      message <- Unmarshal(response.entity).to[String]
    } yield message).andThen {
      case Success(msg) => println(s"Received message: $msg")
      case Failure(err) => println(s"Error: ${err.getMessage}")
    }

    scala.io.StdIn.readLine()



    (update("http://192.168.11.189:50081/api/sql/crmdb/brand",Seq(
      "truncate table simplebrand"
    ))).onComplete{
      case Success(value) => println("update successfully!")
      case Failure(err) => println(s"update error! ${err.getMessage}")
    }
    scala.io.StdIn.readLine()

    val futCreate= update("http://192.168.0.189:50081/api/sql/h2/person",Seq(createCTX))
    futCreate.onComplete{
      case Success(value) => println("table created successfully!")
      case Failure(err) => println(s"table creation error! ${err.getMessage}")
    }
    scala.io.StdIn.readLine()


    val encodedSelect = URLEncode.encode("select id as code, name as fullname from members")
    val encodedInsert = URLEncode.encode("insert into person(fullname,code) values(?,?)")
    val getMembers = HttpRequest(
       HttpMethods.GET,
       uri = "http://192.168.0.189:50081/api/sql/h2/members?sqltext="+encodedSelect
      ).addHeader(authentication)
    val postRequest = HttpRequest(
      HttpMethods.POST,
      uri = "http://192.168.0.189:50081/api/sql/h2/person?sqltext="+encodedInsert,
    ).addHeader(authentication)


    (for {
   //   _ <- update("http://192.168.0.189:50081/api/sql/h2/person",Seq(createCTX))
      respMembers <- Http().singleRequest(getMembers)
      message <- Future.successful(toJson(List(a,b))     "{"name",dsfaUnmarshal(respMembers.entity).to[String]
      reqEntity <- Marshal(message).to[RequestEntity]
      respInsert <- Http().singleRequest(postRequest.copy(entity = reqEntity))
 //       HttpEntity(ContentTypes.`application/json`,ByteString(message))))
    } yield respInsert).onComplete {
      case Success(r@HttpResponse(StatusCodes.OK, _, entity, _)) =>
        println("builk insert successful!")
      case Success(_) => println("builk insert failed!")
      case Failure(err) => println(s"Error: ${err.getMessage}")
    }

    scala.io.StdIn.readLine()


    (for {
      response <- Http().singleRequest(getAllRequest)
      rows <- Unmarshal(response.entity).to[List[Brand]]
      reqEntity <- Marshal(rows.take(1000)).to[RequestEntity]
      resp <- Http().singleRequest(postRequest.copy(entity = reqEntity))
    } yield resp).onComplete {
          case Success(r@HttpResponse(StatusCodes.OK, _, entity, _)) =>
            println("builk insert successful!")
          case Success(_) => println("builk insert failed!")
          case Failure(err) => println(s"Error: ${err.getMessage}")
    }


    def brandToByteString(c: Brand) = {
      ByteString(c.toJson.toString)
    }
    val flowBrandToByteString : Flow[Brand,ByteString,NotUsed] = Flow.fromFunction(brandToByteString)
/*
    val data = Source.fromIterator(()=>rr.iterator).via(flowBrandToByteString)
    data.runForeach(println)
*/
    val data = rr.map(r => ByteString(r.toJson.toString()))

    (for {
      response <- Http().singleRequest(getAllRequest)
      rows <- Unmarshal(response.entity).to[List[Brand]]
      resp <- Http().singleRequest(postRequest.copy(entity = HttpEntity(
        ContentTypes.`application/json`, ByteString(rr.toJson.toString())
      )))
    } yield resp).onComplete {
      case Success(r@HttpResponse(StatusCodes.OK, _, entity, _)) =>
        println("builk insert successful!")
      case Success(_) => println("builk insert failed!")
      case Failure(err) => println(s"Error: ${err.getMessage}")
    }

    scala.io.StdIn.readLine()





    val futResult = (Http().singleRequest(getAllRequest))
      .onComplete {
        case Success(r @ HttpResponse(StatusCodes.OK, _, entity, _)) =>
          entity.dataBytes.map(_.utf8String).flatMapConcat(_.to[Brand]).runForeach(println)
        case Success(r @ HttpResponse(code, _, _, _)) =>
          println(s"Download request failed, response code: $code")
          r.discardEntityBytes()
        case Success(_) => println("Unable to download rows!")
        case Failure(err) => println(s"Download failed: ${err.getMessage}")
      }
    scala.io.StdIn.readLine()


    val futPut = update("http://192.168.11.189:50081/api/sql/termtxns/brand",
      Seq("UPDATE BRAND SET DISC=100 WHERE BRAND='000001'"))
    futPut.onComplete{
      case Success(value) => println("update successfully!")
      case Failure(err) => println(s"update error! ${err.getMessage}")
    }
    scala.io.StdIn.readLine()
*/

    system.terminate()


  }




}