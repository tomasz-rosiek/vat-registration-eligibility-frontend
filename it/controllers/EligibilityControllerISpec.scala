/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import models.S4LKey
import models.api.VatServiceEligibility
import org.jsoup.Jsoup
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import support.AppAndStubs
import play.api.http._
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global

class EligibilityControllerISpec extends PlaySpec with AppAndStubs with ScalaFutures {

  "Eligibility questions on GET" should {
    "return 200" when {
      "[Q1] The user is authorised, current prof is setup, vatscheme is blank,audit is successful, s4l 404's" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility")).isEmpty
          .audit.writesAudit()
        val response = buildClient("/national-insurance-number").get()
        whenReady(response)(_.status) mustBe 200
      }
    }
    "return 200" when {
      "[Q2] the user is authorised, current prof is setup, vatscheme is blank,audit fails, s4l returns valid data" in {

        val s4lData = Json.obj(
          "vatEligibility" -> Json.obj(
            "haveNino"            -> true,
            "doingBusinessAbroad" -> true
          )
        )

        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.failsToWriteAudit()
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility")).contains(s4lData)

        val response = buildClient("/international-business").get()
        val res = whenReady(response)(a => a)
        res.status mustBe 200
        val document = Jsoup.parse(res.body)
        document.title() mustBe "Will the company do international business once it's registered for VAT?"
        document.getElementById("doingBusinessAbroadRadio-true").attr("checked") mustBe "checked"
      }
    }
    "return 404" when {
      "[Q3] the user is not authorised to view the page but session is valid" in {
        given()
          .user.isNotAuthorised
          .audit.writesAudit()

        val response = buildClient("""/involved-more-business-changing-status"""").get()
        whenReady(response)(_.status) mustBe 404
      }
    }
    "return 303" when {
      "[Q4] the user is authorised but the request is invalid" in {
        given()
          .user.isNotAuthorised
          .audit.writesAudit()

        val response = buildClient("/agricultural-flat-rate")(HeaderNames.COOKIE -> "foo").get()
        whenReady(response)(_.status) mustBe 303
      }
    }
    "return 200" when {
      "[Q5] the user is authorised and data is in vat reg backend. data is pulled from vat because S4l returns 404" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.hasValidEligibilityData
          .audit.writesAudit()
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility")).isEmpty

        val response = buildClient("/apply-for-any").get()
        whenReady(response) { res =>
          res.status mustBe 200
          Jsoup.parse(res.body).getElementById("companyWillDoAnyOfRadio-false").attr("checked") mustBe "checked"
        }
      }
    }
  }
  "Eligibility questions on POST" should {
    "return 303 and redirects to next page of questions" when {
      "[Q1] the user submits an answer" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility"))
          .contains(VatServiceEligibility(haveNino = Some(false)))
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility"))
          .isUpdatedWith(VatServiceEligibility(haveNino = Some(false)))(S4LKey("VatServiceEligibility"),VatServiceEligibility.format)

        val response = buildClient("/national-insurance-number").post(Map("haveNinoRadio" -> Seq("true")))
        val res = whenReady(response)(a => a)
        res.status mustBe 303
        res.header(HeaderNames.LOCATION) mustBe Some("/check-if-you-can-register-for-vat/international-business")
      }
    }
    "return 303 and redirects to next page of you are not eligible" when {
      "[Q1] the user submits an answer which is false" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility"))
          .contains(VatServiceEligibility(haveNino = Some(false)))
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility"))
          .isUpdatedWith(VatServiceEligibility(haveNino = Some(false)))(S4LKey("VatServiceEligibility"),VatServiceEligibility.format)
          .keystoreS.putKeyStoreValue("ineligibility-reason",""""haveNino"""")

        val response = buildClient("/national-insurance-number").post(Map("haveNinoRadio" -> Seq("false")))
        val res = whenReady(response)(a => a)
        res.status mustBe 303
        res.header(HeaderNames.LOCATION) mustBe Some("/check-if-you-can-register-for-vat/cant-register")
      }
    }
    "return 400 when invalid form is posted" when {
      "[Q5] the user submits an incorrect value in the form" in {
        given()
          .user.isAuthorised
          .currentProfile.withProfile
          .vatScheme.isBlank
          .audit.writesAudit()
          .s4lContainer[VatServiceEligibility](S4LKey("VatServiceEligibility")).isEmpty

        val response = buildClient("/apply-for-any").post(Map("haveNinoRadio" -> Seq("fooBars")))
        val res = whenReady(response)(a => a)
        res.status mustBe 400
      }
    }

  }
}
