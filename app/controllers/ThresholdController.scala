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

import forms.{ExpectationThresholdForm, OverThresholdFormFactory}
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.hasIncorpDate
import play.api.i18n.MessagesApi
import play.api.mvc._
import services._
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.SessionProfile

import scala.concurrent.Future

class ThresholdControllerImpl @Inject()(val messagesApi: MessagesApi,
                                        val authConnector: AuthConnector,
                                        val currentProfileService: CurrentProfileService,
                                        val eligibilityService: EligibilityService) extends ThresholdController

trait ThresholdController extends VatRegistrationController with SessionProfile {
  val eligibilityService: EligibilityService

  def goneOverShow: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => {
        withCurrentProfile { implicit profile =>
          hasIncorpDate.unapply.flatMap { incorpDate =>
            eligibilityService.getEligibilityChoice map { choice =>
              val overThresholdView = choice.overThreshold.fold(OverThresholdFormFactory.form(incorpDate))(OverThresholdFormFactory.form(incorpDate).fill)
              val incorpDateView    = incorpDate.format(FORMAT_DD_MMMM_Y)
              Ok(views.html.pages.over_threshold(overThresholdView, incorpDateView))
            }
          }
        }
      }
  }

  def goneOverSubmit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          hasIncorpDate.unapply.flatMap { incorpDate =>
            OverThresholdFormFactory.form(incorpDate).bindFromRequest().fold(
              badForm => Future.successful(BadRequest(views.html.pages.over_threshold(badForm, incorpDate.format(FORMAT_DD_MMMM_Y)))),
              data    => eligibilityService.saveChoiceQuestion(data) map {
                _ => Redirect(controllers.routes.ThresholdController.expectationOverShow())
              }
            )
          }
        }
  }

  def expectationOverShow: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          hasIncorpDate.unapply.flatMap { incorpDate =>
            eligibilityService.getEligibilityChoice map { choice =>
              val expectedOvertThresholdView =
                choice.expectationOverThreshold.fold(ExpectationThresholdForm.form(incorpDate))(ExpectationThresholdForm.form(incorpDate).fill)
              Ok(views.html.pages.expectation_over_threshold(expectedOvertThresholdView))
            }
          }
        }
  }

  def expectationOverSubmit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          hasIncorpDate.unapply.flatMap { incorpDate =>
            ExpectationThresholdForm.form(incorpDate).bindFromRequest().fold(
              badForm => Future.successful(BadRequest(views.html.pages.expectation_over_threshold(badForm))),
              data => eligibilityService.saveChoiceQuestion(data) map {
                _ => Redirect(controllers.routes.ThresholdSummaryController.show())
              }
            )
          }
        }
  }
}
