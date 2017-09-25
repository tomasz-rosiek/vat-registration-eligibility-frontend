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

import org.apache.commons.lang3.StringUtils
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services.{CurrentProfileService, IncorpInfoService}
import utils.SessionProfile

import scala.concurrent.Future

@Singleton
class EligibilitySuccessController @Inject()(implicit val messagesApi: MessagesApi,
                                             val currentProfileService: CurrentProfileService,
                                             val incorpInfoService: IncorpInfoService)
  extends VatRegistrationController with SessionProfile {

  def show: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { _ =>
          Future.successful(Ok(views.html.pages.eligible()))
        }
  }

  def submit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { _ =>
          incorpInfoService.fetchIncorporationInfo.subflatMap(_.statusEvent.crn).filter(StringUtils.isNotBlank)
            .fold(controllers.routes.TaxableTurnoverController.show())(
              crn =>
                controllers.routes.OverThresholdController.show()).map(Redirect)
        }
  }
}
