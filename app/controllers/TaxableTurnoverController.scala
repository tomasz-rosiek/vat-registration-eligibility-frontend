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

import forms.TaxableTurnoverForm
import models.view.{TaxableTurnover, VoluntaryRegistration}
import models.view.TaxableTurnover.TAXABLE_YES
import models.view.VoluntaryRegistration.REGISTER_NO
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{CurrentProfileService, S4LService, VatRegFrontendService, VatRegistrationService}
import utils.SessionProfile

import scala.concurrent.Future

@Singleton
class TaxableTurnoverController @Inject()(implicit val messagesApi: MessagesApi,
                                          implicit val s4LService: S4LService,
                                          implicit val vrs: VatRegistrationService,
                                          val currentProfileService: CurrentProfileService,
                                          val vatRegFrontendService: VatRegFrontendService)
  extends VatRegistrationController with SessionProfile {

  import cats.syntax.flatMap._

  val form = TaxableTurnoverForm.form

  def show: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          viewModel[TaxableTurnover]().fold(form)(form.fill)
            .map(f => Ok(views.html.pages.taxable_turnover(f)))
        }
  }

  def submit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          form.bindFromRequest().fold(
            badForm => BadRequest(views.html.pages.taxable_turnover(badForm)).pure,
            (data: TaxableTurnover) => save(data).map(_ => data.yesNo == TAXABLE_YES).ifM(
              for {
                _ <- save(VoluntaryRegistration(REGISTER_NO))
                //_ <- save(StartDateView(COMPANY_REGISTRATION_DATE)) //TODO: call S4LService to save StartDateView
              } yield vatRegFrontendService.buildVatRegFrontendUrlEntry,
              Future.successful(routes.VoluntaryRegistrationController.show.url)
            ) map (Redirect(_)))
        }
  }
}
