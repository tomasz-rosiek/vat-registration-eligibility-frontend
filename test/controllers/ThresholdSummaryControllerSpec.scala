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
import models.api.{VatExpectedThresholdPostIncorp, VatThresholdPostIncorp}
import models.view._
import models.{CurrentProfile, S4LVatEligibilityChoice}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ThresholdSummaryControllerSpec extends VatRegSpec with VatRegistrationFixture with S4LFixture {

  class Setup {

    val testController = new ThresholdSummaryController {
      override val authConnector = mockAuthConnector
      override val eligibilityService = mockEligibilityService
      override val vatRegFrontendService = mockVatRegFrontendService
      override val currentProfileService = mockCurrentProfileService
      override val messagesApi = mockMessages

      override def withCurrentProfile(f: (CurrentProfile) => Future[Result])(implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
        f(currentProfile)
      }
    }
  }

  val fakeRequest = FakeRequest(controllers.routes.ThresholdSummaryController.show())

  "Calling threshold summary to show the threshold summary page" should {
    "return HTML with a valid threshold summary view" in new Setup {

      when(mockEligibilityService.getEligibilityChoice(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithThreshold))

      callAuthorised(testController.show)(_ includesText "Check and confirm your answers")
    }

    "getVatThresholdAndExpectedThreshold returns a valid VatThresholdPostIncorp and VatExpectedThresholdPostIncorp" in new Setup {
      val eligibilityChoice = S4LVatEligibilityChoice(
        expectationOverThreshold = Some(ExpectationOverThresholdView(false,None)),
        overThreshold = Some(OverThresholdView(false))
      )

      when(mockS4LConnector.fetchAndGet[S4LVatEligibilityChoice](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(eligibilityChoice)))

      testController.getVatThresholdAndExpectedThreshold returns (validVatThresholdPostIncorp,VatExpectedThresholdPostIncorp(false,None))
    }

    "mapToModels returns a tuple of VatExpectedThreshold and VatThreshold" in new Setup {
      testController.mapToModels(
        Some(OverThresholdView(false,None)),
        Some(ExpectationOverThresholdView(false,None))) shouldBe (VatThresholdPostIncorp(false,None),VatExpectedThresholdPostIncorp(false,None))
    }

    "getThresholdSummary maps a valid VatThresholdSummary object to a Summary object" in new Setup {
      val eligibilityChoice = S4LVatEligibilityChoice(
        overThreshold = Some(OverThresholdView(false, None)),
        expectationOverThreshold = Some(ExpectationOverThresholdView(false, None))
      )

      when(mockS4LConnector.fetchAndGet[S4LVatEligibilityChoice](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(eligibilityChoice)))

      testController.getThresholdSummary.map(summary => summary.sections.length shouldBe 2)
    }
  }

  s"POST ${controllers.routes.ThresholdSummaryController.submit()}" should {
    "redirect the user to the voluntary registration page if both overThreshold and expectationOverThreshold are false" in new Setup {
      when(mockEligibilityService.getEligibilityChoice(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithThreshold))

      callAuthorised(testController.submit) {
        _ redirectsTo controllers.routes.VoluntaryRegistrationController.show.url
      }
    }

    "redirect the user to the completion capacity page if overThreshold is true" in new Setup {
      val overThreshold = OverThresholdView(true, Some(testDate))

      when(mockEligibilityService.getEligibilityChoice(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithThreshold.copy(overThreshold = Some(overThreshold))))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry)
        .thenReturn("someEntryUrl")

      callAuthorised(testController.submit) {
        _ redirectsTo s"someEntryUrl"
      }
    }

    "redirect the user to the completion capacity page if expectationOverThreshold is true" in new Setup {
      val expectation = ExpectationOverThresholdView(true, Some(testDate))

      when(mockEligibilityService.getEligibilityChoice(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(validS4LEligibilityChoiceWithThreshold.copy(expectationOverThreshold = Some(expectation))))

      when(mockVatRegFrontendService.buildVatRegFrontendUrlEntry)
        .thenReturn("someEntryUrl")

      callAuthorised(testController.submit) {
        _ redirectsTo s"someEntryUrl"
      }
    }
  }
}
