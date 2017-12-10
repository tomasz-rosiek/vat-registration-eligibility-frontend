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

import common.enums.CacheKeys.IneligibilityReason
import common.enums.{EligibilityQuestions => Questions}
import connectors.KeystoreConnector
import forms.ServiceCriteriaFormFactory
import models.CurrentProfile
import models.view.YesOrNoQuestion
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services.{CurrentProfileService, EligibilityService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.SessionProfile

import scala.concurrent.Future


class EligibilityControllerImpl @Inject()(val keystoreConnector: KeystoreConnector,
                                          val currentProfileService: CurrentProfileService,
                                          val eligibilityService: EligibilityService,
                                          val messagesApi: MessagesApi,
                                          val authConnector: AuthConnector) extends EligibilityController

trait EligibilityController extends VatRegistrationController with SessionProfile {
  val eligibilityService: EligibilityService
  val keystoreConnector: KeystoreConnector

  private def submitQuestion(question: Questions.Value, newValue: Boolean, exitCondition: Boolean)(success: => Result, fail: => Result)
                            (implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[Result] = {
    eligibilityService.saveQuestion(question, newValue) flatMap { _ =>
      if(exitCondition) {
        keystoreConnector.cache(IneligibilityReason.toString, question.toString) map (_ => fail)
      } else {
        Future.successful(success)
      }
    }
  }

  private def fillYesNoQuestionForm(question: Questions.Value, optBoolean: Option[Boolean]): Form[YesOrNoQuestion] = {
    val form: Form[YesOrNoQuestion] = ServiceCriteriaFormFactory.form(question)
    optBoolean.fold(form)(x => form.fill(YesOrNoQuestion(question, x)))
  }

  def ineligible(): Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          keystoreConnector.fetchAndGet[String](IneligibilityReason.toString) map {
            case None         => InternalServerError
            case Some(v) if v == Questions.applyingForVatExemption.toString => Ok(views.html.pages.ineligible.exemption_ineligible())
            case Some(v)      => Ok(views.html.pages.ineligible.ineligible(v.toString))
          }
        }
  }

  def showHaveNino : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          for {
            eligibility <- eligibilityService.getEligibility
            formFilled  =  fillYesNoQuestionForm(Questions.haveNino, eligibility.haveNino)
          } yield Ok(views.html.pages.have_nino(formFilled))
        }
  }

  def submitHaveNino: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.haveNino).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.have_nino(hasErrors))),
            data => submitQuestion(Questions.haveNino, data.answer, !data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showDoingBusinessAbroad()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
        }
  }

  def showDoingBusinessAbroad : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          for {
            eligibility <- eligibilityService.getEligibility
            formFilled = fillYesNoQuestionForm(Questions.doingBusinessAbroad, eligibility.doingBusinessAbroad)
          } yield Ok(views.html.pages.doing_business_abroad(formFilled))
        }
  }

  def submitDoingBusinessAbroad: Action[AnyContent] = authorised.async {
    implicit  user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.doingBusinessAbroad).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.doing_business_abroad(hasErrors))),
            data => submitQuestion(Questions.doingBusinessAbroad, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showDoAnyApplyToYou()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
        }
  }

  def showDoAnyApplyToYou : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          for {
            eligibility <- eligibilityService.getEligibility
            formFilled  =  fillYesNoQuestionForm(Questions.doAnyApplyToYou, eligibility.doAnyApplyToYou)
          } yield Ok(views.html.pages.do_any_apply_to_you(formFilled))
        }
  }

  def submitDoAnyApplyToYou: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.doAnyApplyToYou).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.do_any_apply_to_you(hasErrors))),
            data => submitQuestion(Questions.doAnyApplyToYou, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showApplyingForAnyOf()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
        }
  }

  def showApplyingForAnyOf : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          for {
            eligibility <- eligibilityService.getEligibility
            formFilled  =  fillYesNoQuestionForm(Questions.applyingForAnyOf, eligibility.applyingForAnyOf)
          } yield Ok(views.html.pages.applying_for_any_of(formFilled))
        }
  }

  def submitApplyingForAnyOf: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile{ implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.applyingForAnyOf).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.applying_for_any_of(hasErrors))),
            data => submitQuestion(Questions.applyingForAnyOf, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showExemptionCriteria()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
        }
  }

  def showCompanyWillDoAnyOf : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          for {
            eligibility <- eligibilityService.getEligibility
            formFilled  =  fillYesNoQuestionForm(Questions.companyWillDoAnyOf, eligibility.companyWillDoAnyOf)
          } yield Ok(views.html.pages.company_will_do_any_of(formFilled))
        }
  }

  def submitCompanyWillDoAnyOf: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.companyWillDoAnyOf).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.company_will_do_any_of(hasErrors))),
            data => submitQuestion(Questions.companyWillDoAnyOf, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilitySummaryController.show()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
        }
  }

  def showExemptionCriteria : Action[AnyContent] = authorised.async{
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          for {
            eligibility <- eligibilityService.getEligibility
            formFilled  =  fillYesNoQuestionForm(Questions.applyingForVatExemption, eligibility.applyingForVatExemption)
          } yield Ok(views.html.pages.applying_for_vat_exemption(formFilled))
        }
  }

  def submitExemptionCriteria: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          ServiceCriteriaFormFactory.form(Questions.applyingForVatExemption).bindFromRequest.fold(
            hasErrors => Future.successful(BadRequest(views.html.pages.applying_for_vat_exemption(hasErrors))),
            data => submitQuestion(Questions.applyingForVatExemption, data.answer, data.answer)(
              success = Redirect(controllers.routes.EligibilityController.showCompanyWillDoAnyOf()),
              fail    = Redirect(controllers.routes.EligibilityController.ineligible())
            )
          )
        }
  }
}


