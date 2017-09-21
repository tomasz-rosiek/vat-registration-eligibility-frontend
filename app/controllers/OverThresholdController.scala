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

import javax.inject.{Inject, Singleton}

import cats.syntax.FlatMapSyntax
import forms.OverThresholdFormFactory
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.view.OverThreshold
import play.api.i18n.MessagesApi
import play.api.mvc._
import services._
import utils.SessionProfile

@Singleton
class OverThresholdController @Inject()(val formFactory: OverThresholdFormFactory,
                                        val incorpInfoService: IncorpInfoService,
                                        implicit val messagesApi: MessagesApi,
                                        implicit val s4LService: S4LService,
                                        implicit val vrs: VatRegistrationService)
  extends VatRegistrationController with FlatMapSyntax with SessionProfile {

  def show: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => {
        withCurrentProfile { implicit profile =>
          val dateOfIncorporation = profile.incorporationDate
            .getOrElse(throw new IllegalStateException("Date of Incorporation data expected to be found in Incorporation"))

          viewModel[OverThreshold]().fold(formFactory.form(dateOfIncorporation))(formFactory.form(dateOfIncorporation).fill) map {
            form => Ok(views.html.pages.over_threshold(form, dateOfIncorporation.format(FORMAT_DD_MMMM_Y)))
          }
        }
    }
  }

  def submit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          incorpInfoService.fetchDateOfIncorporation().flatMap(date =>
            formFactory.form(date).bindFromRequest().fold(badForm =>
              BadRequest(features.tradingDetails.views.html.vatChoice.over_threshold(badForm, date.format(FORMAT_DD_MMMM_Y))).pure,
              data => save(data).map(_ => Redirect(controllers.vatTradingDetails.vatChoice.routes.ThresholdSummaryController.show()))
            )
          )
        }
  }

}
