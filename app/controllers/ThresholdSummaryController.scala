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

import javax.inject.Inject

import controllers.builders.SummaryVatThresholdBuilder
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.api.{VatExpectedThresholdPostIncorp, VatThresholdPostIncorp}
import models.view.{ExpectationOverThresholdView, OverThresholdView, Summary}
import models.{CurrentProfile, hasIncorpDate}
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CurrentProfileService, EligibilityService, VatRegFrontendService, VatRegistrationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.SessionProfile

import scala.concurrent.Future

class ThresholdSummaryControllerImpl @Inject()(val messagesApi: MessagesApi,
                                               val authConnector: AuthConnector,
                                               val vrs: VatRegistrationService,
                                               val currentProfileService: CurrentProfileService,
                                               val vatRegFrontendService: VatRegFrontendService,
                                               val eligibilityService: EligibilityService) extends ThresholdSummaryController

trait ThresholdSummaryController extends VatRegistrationController with SessionProfile {
  val eligibilityService: EligibilityService
  val vatRegFrontendService: VatRegFrontendService

  def show: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          hasIncorpDate.unapply.flatMap { incorpDate =>
            getThresholdSummary map { thresholdSummary =>
              Ok(views.html.pages.threshold_summary(thresholdSummary, incorpDate.format(FORMAT_DD_MMMM_Y)))
            }
          }
        }
  }

  def submit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => {
        withCurrentProfile { implicit profile =>
          eligibilityService.getEligibilityChoice map { choice =>
            if (choice.overThreshold.getOrElse(OverThresholdView(false)).selection ||
              choice.expectationOverThreshold.getOrElse(ExpectationOverThresholdView(false)).selection) {
              Redirect(vatRegFrontendService.buildVatRegFrontendUrlEntry)
            } else {
              Redirect(controllers.routes.VoluntaryRegistrationController.show())
            }
          }
        }
      }
  }

  def getThresholdSummary(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[Summary] = {
    for {
      (threshold,expectedThreshold) <- getVatThresholdAndExpectedThreshold
    } yield thresholdToSummary(threshold, expectedThreshold)
  }

  def thresholdToSummary(vatThresholdPostIncorp: VatThresholdPostIncorp, expectedThresholdPostIncorp: VatExpectedThresholdPostIncorp): Summary = {
    Summary(Seq(SummaryVatThresholdBuilder(Some(vatThresholdPostIncorp), Some(expectedThresholdPostIncorp)).section))
  }

  def getVatThresholdAndExpectedThreshold(implicit hc: HeaderCarrier,
                                          profile: CurrentProfile): Future[(VatThresholdPostIncorp,VatExpectedThresholdPostIncorp)] = {
    eligibilityService.getEligibilityChoice map { vatChoice =>
      mapToModels(vatChoice.overThreshold, vatChoice.expectationOverThreshold)
    }
  }

  def mapToModels(thresholdView: Option[OverThresholdView],
                  expectedThresholdView: Option[ExpectationOverThresholdView]): (VatThresholdPostIncorp, VatExpectedThresholdPostIncorp) = {
    (thresholdView.map(a => VatThresholdPostIncorp(a.selection, a.date)).get,
      expectedThresholdView.map(a => VatExpectedThresholdPostIncorp(a.selection, a.date)).get)
  }
}
