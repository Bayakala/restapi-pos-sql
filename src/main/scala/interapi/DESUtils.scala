package com.datatech.rest.sql

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESKeySpec
import javax.crypto.spec.IvParameterSpec
import MD5Utils._
import org.apache.commons.codec.binary.Base64

object DESUtils {
  def encrypt(message: String, key: String): String = {
    val cipher = Cipher.getInstance("DES/CBC/PKCS5Padding")
    val desKeySpec = new DESKeySpec(key.getBytes)
    val keyFactory = SecretKeyFactory.getInstance("DES")
    val secretKey = keyFactory.generateSecret(desKeySpec)
    val iv = new IvParameterSpec(key.getBytes)
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
    hexify(cipher.doFinal(message.getBytes("GB2312")))
 //   toHexString(cipher.doFinal(message.getBytes("GB2312"))) //"GB2312"
  }

  def decrypt(message: String, key: String): String = {
    val bytesrc = convertHexString(message);
    val cipher = Cipher.getInstance("DES/CBC/PKCS5Padding")
    val desKeySpec = new DESKeySpec(key.getBytes)
    val keyFactory = SecretKeyFactory.getInstance("DES")
    val secretKey = keyFactory.generateSecret(desKeySpec)
    val iv = new IvParameterSpec(key.getBytes)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
    val retByte = cipher.doFinal(bytesrc)
    new String(retByte)
  }
  def toHexString(b: Array[Byte]): String = {
    val hexString = new StringBuffer
    for(i <- 0 until b.length) {
      var plainText = Integer.toHexString(0xff & b(i))
      if (plainText.length < 2) plainText = "0" + plainText
      hexString.append(plainText)
    }
    hexString.toString
  }
  def decodeBase64(base64String: String): Array[Byte] = Base64.decodeBase64(base64String)
  def encodeBase64(bytes: Array[Byte]): String = Base64.encodeBase64String(bytes)

  def convertHexString(ss: String): Array[Byte] = {
    var digest = new Array[Byte](ss.length / 2)
    for(i <- 0 until (ss.length / 2)) {
      val byteString = ss.substring(2 * i, 2 * i + 2)
      val byteValue = Integer.parseInt(byteString, 16)
      digest(i) = byteValue.toByte
    }
    digest
  }

}