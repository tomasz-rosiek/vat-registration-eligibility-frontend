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

import fixtures.{S4LFixture, VatRegistrationFixture}
import helpers.VatRegSpec
import models.CurrentProfile
import models.view.VoluntaryRegistrationReason
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class VoluntaryRegistrationReasonControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LFixture {

  class Setup {
    val testController = new VoluntaryRegistrationReasonController {
      override val authConnector: AuthConnector = mockAuthConnector
      override val eligibilityService = mockEligibilityService
      override val vatRegFrontendService = mockVatRegFrontendService
      override val currentProfileService = mockCurrentProfileService
      override val messagesApi = mockMessages

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  val fakeRequest = FakeRequest(routes.VoluntaryRegistrationReasonController.show())

  s"GET ${routes.VoluntaryRegistrationReasonController.show()}" should {
    val expectedText = "Which one applies to the company?"

    "return HTML Voluntary Registration Reason page with no Selection" in new Setup{
      when(mockEligibilityService.getEligibilityChoice(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithVoluntarilyData.copy(voluntaryRegistrationReason = None)))

      callAuthorised(testController.show()){
        _ includesText expectedText
      }
    }

    "return HTML when there's no data" in new Setup {
      when(mockEligibilityService.getEligibilityChoice(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyS4LEligibilityChoice))

      callAuthorised(testController.show) {
        _ includesText expectedText
      }
    }
  }

  s"POST ${routes.VoluntaryRegistrationReasonController.submit()} with Empty data" should {
    "return 400" in new Setup {
      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
      ))(result => result isA 400)
    }
  }

  s"POST ${routes.VoluntaryRegistrationReasonController.submit()} with Voluntary Registration Reason selected Sells" should {
    "return 303" in new Setup {
      when(mockEligibilityService.saveChoiceQuestion(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithVoluntarilyData))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry)
        .thenReturn(s"someUrl")

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationReasonRadio" -> VoluntaryRegistrationReason.SELLS
      ))(_ redirectsTo s"someUrl")
    }
  }

  s"POST ${routes.VoluntaryRegistrationReasonController.submit()} with Voluntary Registration Reason selected Intends to sell" should {
    "return 303" in new Setup {
      val reasonIntendsToSell = VoluntaryRegistrationReason(VoluntaryRegistrationReason.INTENDS_TO_SELL)

      when(mockEligibilityService.saveChoiceQuestion(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithVoluntarilyData.copy(voluntaryRegistrationReason = Some(reasonIntendsToSell))))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry)
        .thenReturn(s"someUrl")

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationReasonRadio" -> VoluntaryRegistrationReason.INTENDS_TO_SELL
      )){_ redirectsTo s"someUrl"}
    }
  }

  s"POST ${routes.VoluntaryRegistrationReasonController.submit()} with Voluntary Registration selected Neither" should {
    "redirect to the welcome page" in new Setup {
      val reasonNeither = VoluntaryRegistrationReason(VoluntaryRegistrationReason.NEITHER)

      when(mockEligibilityService.saveChoiceQuestion(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithVoluntarilyData.copy(voluntaryRegistrationReason = Some(reasonNeither))))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlWelcome)
        .thenReturn(s"someUrl")

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationReasonRadio" -> VoluntaryRegistrationReason.NEITHER
      ))(_ redirectsTo "someUrl")
    }
  }
}
