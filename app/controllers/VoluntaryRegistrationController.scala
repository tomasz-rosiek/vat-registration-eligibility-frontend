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

import forms.VoluntaryRegistrationForm
import models.view.VoluntaryRegistration
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CurrentProfileService, S4LService, VatRegFrontendService, VatRegistrationService}
import utils.SessionProfile

@Singleton
class VoluntaryRegistrationController @Inject()(implicit val messagesApi: MessagesApi,
                                                implicit val s4l: S4LService,
                                                val currentProfileService: CurrentProfileService,
                                                val vrs: VatRegistrationService,
                                                val vatRegFrontendService: VatRegFrontendService)
  extends VatRegistrationController with SessionProfile {

  import cats.syntax.flatMap._

  val form = VoluntaryRegistrationForm.form

  def show: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          viewModel[VoluntaryRegistration]().fold(form)(form.fill)
            .map(f => Ok(views.html.pages.voluntary_registration(f)))
        }
  }

  def submit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          form.bindFromRequest().fold(
            badForm => BadRequest(views.html.pages.voluntary_registration(badForm)).pure,
            goodForm => (VoluntaryRegistration.REGISTER_YES == goodForm.yesNo).pure.ifM(
              save(goodForm).map(_ => controllers.routes.VoluntaryRegistrationReasonController.show.url),
              s4l.clear().flatMap(_ => vrs.deleteVatScheme()).map(_ => vatRegFrontendService.buildVatRegFrontendUrlWelcome)
            ).map(Redirect(_)))
        }
  }
}
