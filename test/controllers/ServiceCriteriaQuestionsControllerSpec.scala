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

import common.enums.CacheKeys.IneligibilityReason
import connectors.KeystoreConnector
import fixtures.VatRegistrationFixture
import helpers.{S4LMockSugar, VatRegSpec}
import models.CurrentProfile
import models.api.VatServiceEligibility
import models.view.EligibilityQuestion
import models.view.EligibilityQuestion._
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito._
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{charset, contentAsString, contentType}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import play.api.mvc.Results.Ok
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class ServiceCriteriaQuestionsControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LMockSugar {

  class Setup {
    val testController = new ServiceCriteriaQuestionsController(mockKeystoreConnector,
      mockCurrentProfileService,
      mockVatRegFrontendService,
      mockMessages,
      mockVatRegistrationService,
      mockS4LService) {
      override val authConnector: AuthConnector = mockAuthConnector

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  private def setupIneligibilityReason(keystoreConnector: KeystoreConnector, question: EligibilityQuestion) =
    when(mockKeystoreConnector.fetchAndGet[String](Matchers.eq(IneligibilityReason.toString))(any(), any()))
      .thenReturn(Some(question.name).pure)

  "GET ServiceCriteriaQuestionsController.show()" should {
    "return HTML for relevant page with no data in the form" in new Setup {
      save4laterReturnsViewModel[VatServiceEligibility](validServiceEligibility)()

      val eligibilityQuestions = Seq[(EligibilityQuestion, String)](
        HaveNinoQuestion -> "Do you have a National Insurance number?",
        DoingBusinessAbroadQuestion -> "Will the company do international business",
        DoAnyApplyToYouQuestion -> "Are you involved with more than one business or changing the legal status of your business?",
        ApplyingForAnyOfQuestion -> "Is the company applying for either the Agricultural Flat Rate Scheme or the Annual Accounting Scheme?",
        CompanyWillDoAnyOfQuestion -> "Will the company do any of the following once"
      )

      forAll(eligibilityQuestions) { case (question, expectedTitle) =>
        callAuthorised(testController.show(question.name))(_ includesText expectedTitle)
      }
    }

  }

  "POST ServiceCriteriaQuestionsController.submit()" should {
    //TODO: fix reverse route when exec unit test with testOnly - SCRS-9055
    def addPrefix(url: String): String = {
      if (!url.startsWith("/check-if-you-can-register-for-vat")) {
        s"/check-if-you-can-register-for-vat$url"
      } else url
    }

    def urlForQuestion(eligibilityQuestion: EligibilityQuestion): String = {
      addPrefix(controllers.routes.ServiceCriteriaQuestionsController.show(eligibilityQuestion.name).url)
    }

    val questions = Seq(
      HaveNinoQuestion -> urlForQuestion(DoingBusinessAbroadQuestion),
      DoingBusinessAbroadQuestion -> urlForQuestion(DoAnyApplyToYouQuestion),
      DoAnyApplyToYouQuestion -> urlForQuestion(ApplyingForAnyOfQuestion),
      ApplyingForAnyOfQuestion -> urlForQuestion(CompanyWillDoAnyOfQuestion),
      CompanyWillDoAnyOfQuestion -> addPrefix(controllers.routes.EligibilitySuccessController.show().url)
    )

    println(questions)

    "redirect to next screen when user is eligible to register for VAT using this service" in new Setup {
      when(mockVatRegistrationService.submitVatEligibility()(any(),any()))
        .thenReturn(validServiceEligibility.pure)

      forAll(questions) { case (currentQuestion, nextScreenUrl) =>
        setupIneligibilityReason(mockKeystoreConnector, currentQuestion)
        save4laterReturnsViewModel(validServiceEligibility)()
        save4laterExpectsSave[VatServiceEligibility]()

        submitAuthorised(testController.submit(currentQuestion.name),
          FakeRequest().withFormUrlEncodedBody(
            "question" -> currentQuestion.name,
            s"${currentQuestion.name}Radio" -> (!currentQuestion.exitAnswer).toString)
        )(_ redirectsTo nextScreenUrl)
      }
    }

    "redirect to next screen when eligible and nothing in s4l or backend" in new Setup {
      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any()))
        .thenReturn(Future.successful(currentProfile))

      when(mockVatRegistrationService.submitVatEligibility()(any(),any())).thenReturn(validServiceEligibility.pure)
      forAll(questions) { case (currentQuestion, nextScreenUrl) =>
        setupIneligibilityReason(mockKeystoreConnector, currentQuestion)
        save4laterReturnsNoViewModel[VatServiceEligibility]()
        when(mockVatRegistrationService.getVatScheme()(any(),any())).thenReturn(validVatScheme.copy(vatServiceEligibility = None).pure)
        save4laterExpectsSave[VatServiceEligibility]()

        submitAuthorised(testController.submit(currentQuestion.name),
          FakeRequest().withFormUrlEncodedBody(
            "question" -> currentQuestion.name,
            s"${currentQuestion.name}Radio" -> (!currentQuestion.exitAnswer).toString)
        )(_ redirectsTo nextScreenUrl)
      }
    }

    "redirect to ineligible screen when user is NOT eligible to register for VAT using this service" in new Setup {
      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any()))
        .thenReturn(Future.successful(currentProfile))

      when(mockVatRegistrationService.submitVatEligibility()(any(),any())).thenReturn(validServiceEligibility.pure)
      save4laterReturnsViewModel(validServiceEligibility)()
      save4laterExpectsSave[VatServiceEligibility]()
      when(mockKeystoreConnector.cache[String](any(), any())(any(), any())).thenReturn(CacheMap("id", Map()).pure)

      forAll(questions) { case (currentQuestion, _) =>
        setupIneligibilityReason(mockKeystoreConnector, currentQuestion)

        submitAuthorised(testController.submit(currentQuestion.name),
          FakeRequest().withFormUrlEncodedBody(
            "question" -> currentQuestion.name,
            s"${currentQuestion.name}Radio" -> currentQuestion.exitAnswer.toString)
        ) {
          _ redirectsTo controllers.routes.ServiceCriteriaQuestionsController.ineligible().url
        }

      }
      verify(mockKeystoreConnector, times(questions.size)).cache[String](Matchers.eq(IneligibilityReason.toString), any())(any(), any())
    }

    "400 for malformed requests" in new Setup {
      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any()))
        .thenReturn(Future.successful(currentProfile))

      forAll(questions) { case (q, _) =>
        submitAuthorised(testController.submit(q.name),
          FakeRequest().withFormUrlEncodedBody(s"${q.name}Radio" -> "foo")
        )(result => result isA 400)
      }
    }
  }


  "GET ineligible screen" should {

    "return HTML for relevant ineligibility page" in new Setup {

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any()))
        .thenReturn(Future.successful(currentProfile))

      //below the "" empty css class indicates that the section is showing (not "hidden")
      val eligibilityQuestions = Seq[(EligibilityQuestion, String)](
        HaveNinoQuestion -> """id="nino-text" class=""""",
        DoingBusinessAbroadQuestion -> """id="business-abroad-text" class=""""",
        DoAnyApplyToYouQuestion -> """id="do-any-apply-to-you-text" class=""""",
        ApplyingForAnyOfQuestion -> """id="applying-for-any-of-text" class=""""",
        CompanyWillDoAnyOfQuestion -> """id="company-will-do-any-of-text" class="""""
      )

      forAll(eligibilityQuestions) { case (question, expectedTitle) =>
        setupIneligibilityReason(mockKeystoreConnector, question)
        callAuthorised(testController.ineligible())(_ includesText expectedTitle)
      }
    }

    "return HTML for ineligibility page even when no reason found in keystore" in new Setup {
      val hiddenReasonSections = Seq(
        """id="nino-text" class="hidden"""",
        """id="business-abroad-text" class="hidden"""",
        """id="do-any-apply-to-you-text" class="hidden"""",
        """id="applying-for-any-of-text" class="hidden"""",
        """id="company-will-do-any-of-text" class="hidden""""
      )

      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any()))
        .thenReturn(Future.successful(currentProfile))

      when(mockKeystoreConnector.fetchAndGet[String](Matchers.eq(IneligibilityReason.toString))(any(), any()))
        .thenReturn(Option.empty[String].pure)
      when(mockVatRegistrationService.getVatScheme()(any(), any())).thenReturn(validVatScheme.copy(vatServiceEligibility = None).pure)

      callAuthorised(testController.ineligible()) {
        result => forAll(hiddenReasonSections)(result includesText _)
      }
    }

  }

}
