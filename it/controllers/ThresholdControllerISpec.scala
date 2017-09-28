package controllers

import models.S4LKey
import models.view.{TaxableTurnover, VoluntaryRegistration}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import support.AppAndStubs


class ThresholdControllerISpec extends PlaySpec with AppAndStubs with ScalaFutures {

  "/vat-taxable-sales-over-threshold GET" should {
    "return 200" when {
      "user is authorised and all conditions are fulfilled" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[TaxableTurnover](S4LKey("VatTradingDetails")).isEmpty

        val response = buildClient("/vat-taxable-sales-over-threshold").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }
  "/vat-taxable-sales-over-threshold POST" should {
    "return 303" when {
      "user successfully submits a valid form" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .s4lContainer[TaxableTurnover](S4LKey("VatTradingDetails")).isEmpty
          .s4lContainer[TaxableTurnover](S4LKey("VatTradingDetails")).isUpdatedWith(TaxableTurnover("TAXABLE_YES"))(S4LKey("VatTradingDetails"),TaxableTurnover.format)
          .s4lContainer[TaxableTurnover](S4LKey("VatTradingDetails")).isUpdatedWith(VoluntaryRegistration("REGISTER_NO"))(S4LKey("VatTradingDetails"),VoluntaryRegistration.format)
          .audit.writesAudit()
          .audit.writesAudit()
         val response = buildClient("/vat-taxable-sales-over-threshold").post(Map("taxableTurnoverRadio" -> Seq("TAXABLE_YES")))
        whenReady(response)(_.status) mustBe 303
      }
    }
  }
  "/gone-over-threshold  GET" should {
    "return 200" when {
      "when user is authorised and has a date of incorporation" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .audit.writesAudit()

        val response = buildClient("/gone-over-threshold").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

}
