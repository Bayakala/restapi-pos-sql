package com.datatech.rest.sql
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import scalikejdbc._

object PosModel {
  val  dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.CHINA)

  sealed trait SqlRow {}
  case class RawTxns(
                      txndate: String = LocalDate.now.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                      ,txntime: String = LocalDateTime.now.format(dateTimeFormatter).substring(11)
                      ,opr: String = ""//工号
                      ,num: Int = 0 //销售单号
                      ,seq: Int = 1 //交易序号
                      ,txntype: Int = 0//交易类型
                      ,salestype: Int = 2 //销售类型
                      ,qty: Int =  1 //交易数量
                      ,price: Int = 0 //单价（分）
                      ,amount: Int = 0 //码洋（分）
                      ,disc: Int = 0   //折扣率 (%) 100% = 1
                      ,dscamt: Int = 0 //折扣额：负值  net实洋 = amount + dscamt
                      ,member: String = "" //会员卡号
                      ,code: String = "" //编号（商品、部类编号、账户编号、卡号...）
                      ,acct: String = "" //账号
                      ,dpt: String = "" //部类
                    ) extends SqlRow


  case class PosTxns (
                       shopid  : String,
                       terminalid  : String,
                       txndate   : String,
                       txntime   : String,
                       opr       : String,
                       txnnum     : String,
                       txnid     : String,
                       num       : Int,
                       seq       : Int,
                       txntype   : Int,
                       salestype : Int,
                       dpt       : String,
                       acct      : String,
                       code      : String,
                       qty       : Int,
                       price     : Int,
                       amount    : Int,
                       disc      : Int,
                       dscamt    : Int,
                       member    : String
                     ) extends SqlRow


  case class Item(
                   dpt: String,
                   name: String
                 )
  case class PayMethod(
                        acct: String,
                        name: String
                      )
  def toItem: WrappedResultSet => Item = rs => Item(
    dpt = rs.string("dpt"),
    name = rs.string("name")
  )
  def toPayMethod: WrappedResultSet => PayMethod = rs => PayMethod(
    acct = rs.string("acct"),
    name = rs.string("name")
  )

  case class MemberInfo(
                         code: String,
                         name: String,
                         phone: String,
                         expdate: String,
                         disc: Int = 0
                       )

  case class Smartcard(
                        code: String,
                        expdate: String,
                        balance: Double = 0.0
                      )

  case class ActivateRequest(
                              deviceId: String,
                              machineName: String,
                              userShop: String,
                              contact: String,
                              dateApplied: String
                            )
  case class MemberData (
                          CODE: String,
                          MEMBER_NAME: String,
                          CPTS: Double,
                          MINPOINT: Double,
                          POINTRATIO: Double,
                          POINTFEE: Double,
                          BALANCE: Double,
                          PHONE: String,
                          BIRTHDAY: String,
                          GRADE: String,
                          EXPDATE: String,
                          STATUS: String
                        )

}
