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

import forms.VoluntaryRegistrationReasonForm
import models.view.VoluntaryRegistrationReason
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CurrentProfileService, S4LService, VatRegFrontendService, VatRegistrationService}
import utils.SessionProfile

@Singleton
class VoluntaryRegistrationReasonController @Inject()(implicit val messagesApi: MessagesApi,
                                                      implicit val s4l: S4LService,
                                                      implicit val vrs: VatRegistrationService,
                                                      val currentProfileService: CurrentProfileService,
                                                      val vatRegFrontendService: VatRegFrontendService)
  extends VatRegistrationController with SessionProfile {

  import cats.syntax.flatMap._

  val form = VoluntaryRegistrationReasonForm.form

  def show: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          viewModel[VoluntaryRegistrationReason]().fold(form)(form.fill)
            .map(f => Ok(views.html.pages.voluntary_registration_reason(f)))
        }
  }

  def submit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          form.bindFromRequest().fold(
            badForm => BadRequest(views.html.pages.voluntary_registration_reason(badForm)).pure,
            goodForm => (goodForm.reason == VoluntaryRegistrationReason.NEITHER).pure.ifM(
              s4l.clear().flatMap(_ => vrs.deleteVatScheme()).map(_ => vatRegFrontendService.buildVatRegFrontendUrlWelcome),
              save(goodForm).map(_ => vatRegFrontendService.buildVatRegFrontendUrlEntry)
            ).map(Redirect(_)))
        }
  }
}
