// Real Stripe webhook signature verification (HMAC-SHA256 + timestamp
// tolerance) — mirrors backend-python's webhook_handler.py.
package com.example.modules.payments.internal

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.time.Instant

object StripeWebhookVerifier:
  private val toleranceSeconds = 300L

  def verify(payload: String, sigHeader: String, secretOpt: Option[String]): Either[String, Unit] =
    secretOpt.filter(_.nonEmpty) match
      case None => Left("STRIPE_WEBHOOK_SECRET is not configured")
      case Some(secret) =>
        val parts = sigHeader.split(",").map(_.split("=", 2)).filter(_.length == 2)
        val timestamp = parts.find(_(0).trim == "t").map(_(1).trim.toLong).getOrElse(0L)
        val sigs = parts.filter(_(0).trim == "v1").map(_(1).trim)
        if timestamp == 0L || sigs.isEmpty then Left("malformed Stripe-Signature header")
        else
          val now = Instant.now.getEpochSecond
          if math.abs(now - timestamp) > toleranceSeconds then Left("timestamp outside tolerance")
          else
            val signedPayload = s"$timestamp.$payload"
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256"))
            val expected = mac.doFinal(signedPayload.getBytes("UTF-8")).map("%02x".format(_)).mkString
            if sigs.exists(s => MessageDigest.isEqual(s.getBytes("UTF-8"), expected.getBytes("UTF-8")))
            then Right(())
            else Left("signature mismatch")
