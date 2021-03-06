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

import auth.VatTaxRegime
import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import config.FrontendAuthConnector
import play.api.data.{Form, FormError}
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.controller.FrontendController

abstract class VatRegistrationController extends FrontendController
  with I18nSupport with Actions with ApplicativeSyntax with FutureInstances {

  override val authConnector: AuthConnector = FrontendAuthConnector

  /**
    * Use this to obtain an [[uk.gov.hmrc.play.frontend.auth.UserActions.AuthenticatedBy]] action builder.
    * Usage of an `AuthenticatedBy` is similar to standard [[play.api.mvc.Action]]. Just like you would do this:
    * {{{Action ( implicit request => Ok(...))}}}
    * or
    * {{{Action.async( implicit request => ??? // generates a Future Result )}}}
    * With `AuthenticatedBy` you would do the same but you get a handle on the current user's [[uk.gov.hmrc.play.frontend.auth.AuthContext]] too:
    * {{{authorised( implicit user => implicit request => Ok(...))}}}
    * or
    * {{{authorised.async( implicit user => implicit request => ??? // generates a Future Result )}}}
    *
    * @return an AuthenticatedBy action builder that is specific to VatTaxRegime and GGConfidence confidence level
    */
  protected[controllers] def authorised: AuthenticatedBy = AuthorisedFor(taxRegime = VatTaxRegime, pageVisibility = GGConfidence)

  protected[controllers] def copyGlobalErrorsToFields[T](globalErrors: String*): Form[T] => Form[T] =
    fwe => fwe.copy(errors = fwe.errors ++ fwe.globalErrors.collect {
      case fe if fe.args.headOption.exists(globalErrors.contains) =>
        FormError(fe.args.head.asInstanceOf[String], fe.message, fe.args)
    })

}
