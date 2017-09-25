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

import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.CurrentProfileService
import utils.SessionProfile

import scala.concurrent.Future

@Singleton
class TwirlViewController @Inject()(implicit val messagesApi: MessagesApi,
                                    val currentProfileService: CurrentProfileService)
  extends VatRegistrationController with SessionProfile {

  def renderViewAuthorised(viewName: String): Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { _ =>
          Future.successful(
            Some(viewName).collect {
              case "use-this-service" => views.html.pages.use_this_service()
            }.fold[Result](NotFound)(Ok(_))
          )
        }
  }
}
