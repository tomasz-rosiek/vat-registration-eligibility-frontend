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

import common.enums.CacheKeys.IneligibilityReason
import cats.data.OptionT
import connectors.KeystoreConnector
import forms.ServiceCriteriaFormFactory
import models.YesOrNoQuestion
import models.api.VatServiceEligibility
import models.view.EligibilityQuestion._
import models.view.EligibilityQuestion
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Call, Request}
import services.{CurrentProfileService, S4LService, VatRegFrontendService, VatRegistrationService}
import utils.SessionProfile

@Singleton
class ServiceCriteriaQuestionsController @Inject()(val keystoreConnector: KeystoreConnector,
                                                   val currentProfileService: CurrentProfileService,
                                                   val vatRegFrontendService: VatRegFrontendService,
                                                   implicit val messagesApi: MessagesApi,
                                                   implicit val vrs: VatRegistrationService,
                                                   implicit val s4l: S4LService)
  extends VatRegistrationController with SessionProfile {

  private def nextQuestion(question: EligibilityQuestion): Call = question match {
    case HaveNinoQuestion            => controllers.routes.ServiceCriteriaQuestionsController.show(DoingBusinessAbroadQuestion.name)
    case DoingBusinessAbroadQuestion => controllers.routes.ServiceCriteriaQuestionsController.show(DoAnyApplyToYouQuestion.name)
    case DoAnyApplyToYouQuestion     => controllers.routes.ServiceCriteriaQuestionsController.show(ApplyingForAnyOfQuestion.name)
    case ApplyingForAnyOfQuestion    => controllers.routes.ServiceCriteriaQuestionsController.show(CompanyWillDoAnyOfQuestion.name)
    case CompanyWillDoAnyOfQuestion  => controllers.routes.EligibilitySuccessController.show
  }

  private def viewForQuestion(q: EligibilityQuestion, form: Form[YesOrNoQuestion])(implicit r: Request[AnyContent]) = q match {
    case HaveNinoQuestion            => views.html.pages.have_nino(form)
    case DoingBusinessAbroadQuestion => views.html.pages.doing_business_abroad(form)
    case DoAnyApplyToYouQuestion     => views.html.pages.do_any_apply_to_you(form)
    case ApplyingForAnyOfQuestion    => views.html.pages.applying_for_any_of(form)
    case CompanyWillDoAnyOfQuestion  => views.html.pages.company_will_do_any_of(form)
  }

  def show(q: String): Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          val question = EligibilityQuestion(q)
          val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(question.name)
          viewModel[VatServiceEligibility]()
            .flatMap(e => OptionT.fromOption(e.getAnswer(question)))
            .fold(form)(answer => form.fill(YesOrNoQuestion(question.name, answer)))
            .map(f => Ok(viewForQuestion(question, f)))
        }
  }

  def submit(q: String): Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          val question = EligibilityQuestion(q)
          import common.ConditionalFlatMap._
          ServiceCriteriaFormFactory.form(question.name).bindFromRequest().fold(
            badForm => BadRequest(viewForQuestion(question, badForm)).pure,
            data => for {
              vatEligibility <- viewModel[VatServiceEligibility]().getOrElse(VatServiceEligibility())
              _ <- save(vatEligibility.setAnswer(question, data.answer))
              exit = data.answer == question.exitAnswer
              _ <- keystoreConnector.cache(IneligibilityReason.toString, question.name) onlyIf exit
              _ <- vrs.submitVatEligibility() onlyIf question == CompanyWillDoAnyOfQuestion
            } yield Redirect(if(exit) routes.ServiceCriteriaQuestionsController.ineligible() else nextQuestion(question)))
        }
  }

  def ineligible(): Action[AnyContent] = authorised.async(implicit user => implicit request =>
    OptionT(keystoreConnector.fetchAndGet[String](IneligibilityReason.toString)).getOrElse("")
      .map(failedQuestion => Ok(views.html.pages.ineligible(failedQuestion))))

}
