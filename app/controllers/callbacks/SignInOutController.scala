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

package controllers.callbacks

import javax.inject.{Inject, Singleton}

import controllers.VatRegistrationController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.config.ServicesConfig

@Singleton
class SignInOutController @Inject()(implicit val messagesApi: MessagesApi) extends VatRegistrationController with ServicesConfig {

  lazy val compRegFEURL = getConfString("company-registration-frontend.www.url", "")
  lazy val compRegFEURI = getConfString("company-registration-frontend.www.uri", "")

  def postSignIn: Action[AnyContent] = authorised(implicit user => implicit request =>
    Redirect(s"$compRegFEURL$compRegFEURI/post-sign-in")
  )

  def signOut: Action[AnyContent] = authorised { implicit user => implicit request =>
    Redirect(s"$compRegFEURL$compRegFEURI/questionnaire").withNewSession
  }

  def dashboard: Action[AnyContent] = authorised { implicit user => implicit request =>
    Redirect(s"$compRegFEURL$compRegFEURI/dashboard")
  }
}
