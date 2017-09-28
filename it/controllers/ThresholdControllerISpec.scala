package controllers

import models.S4LKey
import models.view.TaxableTurnover
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import support.AppAndStubs


class ThresholdControllerISpec  extends PlaySpec with AppAndStubs with ScalaFutures {

  "/vat-taxable-sales-over-threshold GET" should {
    "return 200" when {
      "user is authorised and all conditions are fulfilled" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[TaxableTurnover](S4LKey("vatChoice")).isEmpty
        val response = buildClient("/vat-taxable-sales-over-threshold").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }
  "/vat-taxable-sales-over-threshold POST" should {
    "return 303" when {
      "user successfully submits a valid form" in {

      }
    }
  }

}
