package com.datatech.rest.sql
import akka.stream._   //ActorMaterializer
import akka.stream.scaladsl._
import scala.concurrent._
import Repo._
import akka.http.scaladsl.server.directives.Credentials
import AuthBase._
import scala.concurrent.duration._
import PosModel._
import akka.NotUsed

object PosRepo {
 class PosRepo(implicit ec: ExecutionContextExecutor, mat: Materializer) extends JDBCRepo {
   val rsc = new RSConverter

   def getUserInfo(userid: String): Option[UserInfo] = {
     val sql = "SELECT CUSTOMERS.SHOPID AS SHOPID, TERMINALID, DEVICEID, IMPSVCURL FROM CUSTOMERS INNER JOIN TERMINALS " +
       " ON CUSTOMERS.SHOPID=TERMINALS.SHOPID " +
       " WHERE (CUSTOMERS.DISABLED=0 AND TERMINALS.DISABLED=0) " +
       " AND (CUSTOMERS.EXPDATE > GETDATE() AND TERMINALS.EXPDATE > GETDATE()) AND TERMINALID='" + userid + "'"
     val rows = query("mpos", sql, rsc)
     val futUI: Future[Option[Map[String, Any]]] = rows.runWith(Sink.lastOption)
     Await.result(futUI, 3 seconds)
   }

   def futureUserInfo(userid: String): Future[Option[Map[String, Any]]] = {
     val sql = "SELECT CUSTOMERS.SHOPID AS SHOPID, TERMINALID, DEVICEID, IMPSVCURL FROM CUSTOMERS INNER JOIN TERMINALS " +
       " ON CUSTOMERS.SHOPID=TERMINALS.SHOPID " +
       " WHERE (CUSTOMERS.DISABLED=0 AND TERMINALS.DISABLED=0) " +
       " AND (CUSTOMERS.EXPDATE > GETDATE() AND TERMINALS.EXPDATE > GETDATE()) AND TERMINALID='" + userid + "'"
     val rows = query("mpos", sql, rsc)
     rows.runWith(Sink.lastOption)
   }

   def getValidUser(credentials: Credentials): Option[UserInfo] =
     credentials match {
       case p@Credentials.Provided(_) =>
         val userinfo = getUserInfo(p.identifier)
         userinfo match {
           case Some(ui) => if (p.verify(ui("DEVICEID").toString.trim)) Some(ui) else None
           case _ => None
         }
       case _ => None
     }

 }
  def idsfromJwt(jwt: String)(implicit auth: AuthBase): (String,String) = {
    val oui = auth.getUserInfo(jwt)
    oui match {
      case Some(ui) =>
        try {
          (ui("SHOPID").toString.trim, ui("TERMINALID").toString.trim)
        } catch {case err: Throwable => ("1011","9931")}
      case _ => ("1011","9931")
    }
  }
 def getTxnnum(shopId: String,terminalId: String, txndate: String, vnum: Int, seq: Int): (String,String) = {
   val _shopid = ("0000"+shopId.trim).substring(("0000"+shopId.trim).length - 4)
   val _posid = ("0000"+terminalId.trim).substring(("0000"+terminalId.trim).length - 4)
   val _num = (100000+vnum).toString.substring(1)
   val _seq = (10000+seq).toString.substring(1)
   val txnnum = _shopid + _posid + txndate.trim + _num
   (txnnum,txnnum+_seq)
 }
 def raw2PosTxns(jwt: String,raw: RawTxns)(implicit repo: PosRepo, auth: AuthBase): PosTxns = {
   val (_shopid, _posid) = idsfromJwt(jwt)
   val (_txnnum,_txnid) = getTxnnum(_shopid,_posid,raw.txndate,raw.num,raw.seq)
   PosTxns(
     shopid = _shopid,
     terminalid = _posid,
     txnnum = _txnnum,
     txnid = _txnid,
     opr = raw.opr,
     txndate    = raw.txndate,
     txntime    = raw.txntime,
     num        = raw.num,
     seq        = raw.seq,
     txntype    = raw.txntype,
     salestype  = raw.salestype,
     dpt        = raw.dpt,
     acct       = raw.acct,
     code       = raw.code,
     qty        = raw.qty,
     price      = raw.price,
     amount     = raw.amount,
     disc       = raw.disc,
     dscamt     = raw.dscamt,
     member     = raw.member
   )
 }
 def takeInTxns(jwt: String,json: String, tbl: String)(implicit mat: Materializer, repo: PosRepo, auth: AuthBase) = {
   val lstTxns: List[RawTxns] = fromJson[List[RawTxns]](json)
   val source: Source[PosTxns,NotUsed] = Source.fromIterator(() => lstTxns.iterator).map(r => raw2PosTxns(jwt,r))

   val sqlins = "insert into "+tbl+"(shopid,terminalid,txnnum,txnid,txndate,txntime,opr ,num ,seq ,txntype ,salestype ,qty,price ,amount,disc,dscamt,member,code,acct ,dpt)"
   val sqlparams = " values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
   repo.bulkInsert("mpos",sqlins+sqlparams,mapParams,source)
 }
 def mapParams: PosTxns => Seq[Any] = txns => {
   Seq(
     txns.shopid,txns.terminalid,txns.txnnum,txns.txnid,
     txns.txndate,txns.txntime,txns.opr,txns.num,txns.seq,
     txns.txntype,txns.salestype,txns.qty,txns.price,txns.amount,
     txns.disc,txns.dscamt,txns.member,txns.code,txns.acct ,txns.dpt)
 }


}
