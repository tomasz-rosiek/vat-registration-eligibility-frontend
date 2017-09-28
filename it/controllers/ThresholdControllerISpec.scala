package controllers

import models.{S4LKey, S4LTradingDetails}
import models.view.{OverThresholdView, TaxableTurnover, VoluntaryRegistration}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
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
          .vatScheme.isBlank
          .currentProfile.withProfileAndIncorpDate
          .audit.writesAudit()

        val response = buildClient("/gone-over-threshold").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }

  "/gone-over-threshold POST" should{
    "return 303" when {
      "when the request is valid" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[OverThresholdView](S4LKey("VatTradingDetails")).isEmpty
          .s4lContainer[OverThresholdView](S4LKey("VatTradingDetails")).isUpdatedWith(OverThresholdView(false))(S4LKey("VatTradingDetails"),OverThresholdView.format)
        val response = buildClient("/gone-over-threshold").post(Map("overThresholdRadio" ->Seq("false")))
        whenReady(response)(_.status) mustBe 303
      }
    }
  }
  "/check-confirm-answers GET" should {
    "return 200" when {
      "when the request is valid" in {
        val s4lData = Json.toJson(S4LTradingDetails(Some(
          TaxableTurnover("TAXABLE_YES"))
          ,Some(VoluntaryRegistration("REGISTER_NO"))
          ,None
          ,Some(OverThresholdView(false))))

          given()
          .user.isAuthorised
          .currentProfile.withProfileAndIncorpDate
          .vatScheme.isBlank
          .audit.writesAudit()
            .s4lContainer[S4LTradingDetails](S4LKey("VatTradingDetails")).contains(s4lData)

        val response = buildClient("/check-confirm-answers").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
  }
  "/check-confirm-answers POST" should {
    "return 303" when {
      "when the request is valid" in {

      }
    }
  }
  "/do-you-want-to-register-voluntarily GET" should {
    "return 200" when {
      "when the request is valid" in {

      }
    }
  }
  "/do-you-want-to-register-voluntarily POST" should {
    "return 303" when {
      "when the request is valid" in {

      }
    }
  }
  "/reason-for-registering GET" should {
    "return 200" when {
      "when the request is valid" in {

      }
    }
  }
  "/reason-for-registering POST" should {
    "return 303" when {
      "when the request is valid" in {

      }
    }
  }

}
