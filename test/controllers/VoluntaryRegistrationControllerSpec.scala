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
import models.view.VoluntaryRegistration
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class VoluntaryRegistrationControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LFixture {

  class Setup {
    val testController = new VoluntaryRegistrationController {
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

  val fakeRequest = FakeRequest(routes.VoluntaryRegistrationController.show())

  s"GET ${routes.VoluntaryRegistrationController.show()}" should {
    val expectedText = "Do you want to register voluntarily for VAT?"

    "return HTML Voluntary Registration page with no Selection" in new Setup {
      when(mockEligibilityService.getEligibilityChoice(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(emptyS4LEligibilityChoice))

      callAuthorised(testController.show()) {
        _ includesText expectedText
      }
    }

    "return HTML when there's view data" in new Setup {
      when(mockEligibilityService.getEligibilityChoice(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithVoluntarilyData))

      callAuthorised(testController.show) {
        _ includesText expectedText
      }
    }
  }

  s"POST ${routes.VoluntaryRegistrationController.submit()} with Empty data" should {
    "return 400" in new Setup {
      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody()) {
        result => result isA 400
      }
    }
  }

  s"POST ${routes.VoluntaryRegistrationController.submit()} with Voluntary Registration selected Yes" should {
    "return 303" in new Setup {
      when(mockEligibilityService.saveChoiceQuestion(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithVoluntarilyData))

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationRadio" -> VoluntaryRegistration.REGISTER_YES
      ))(_ redirectsTo controllers.routes.VoluntaryRegistrationReasonController.show().url)
    }
  }

  s"POST ${routes.VoluntaryRegistrationController.submit()} with Voluntary Registration selected No" should {
    "redirect to the welcome page" in new Setup {
      import models.view.VoluntaryRegistration.REGISTER_NO

      val registerNO = VoluntaryRegistration(REGISTER_NO)

      when(mockEligibilityService.saveChoiceQuestion(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithVoluntarilyData.copy(voluntaryRegistration = Some(registerNO))))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlWelcome)
        .thenReturn(s"someUrl")

      submitAuthorised(testController.submit(), fakeRequest.withFormUrlEncodedBody(
        "voluntaryRegistrationRadio" -> VoluntaryRegistration.REGISTER_NO
      ))(_ redirectsTo "someUrl")
    }
  }
}
