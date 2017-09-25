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

import connectors.KeystoreConnector
import controllers.builders.SummaryVatThresholdBuilder
import models.api.VatThresholdPostIncorp
import models.view.{Summary, VoluntaryRegistration}
import models.view.VoluntaryRegistration.REGISTER_NO
import models.{CurrentProfile, MonthYearModel, S4LTradingDetails}
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CurrentProfileService, S4LService, VatRegFrontendService, VatRegistrationService}
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.SessionProfile

import scala.concurrent.Future

class ThresholdSummaryController @Inject()(implicit val messagesApi: MessagesApi,
                                           implicit val s4LService: S4LService,
                                           implicit val vrs: VatRegistrationService,
                                           val currentProfileService: CurrentProfileService,
                                           val vatRegFrontendService: VatRegFrontendService)
  extends VatRegistrationController with SessionProfile {

  def show: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request =>
        withCurrentProfile { implicit profile =>
          val dateOfIncorporation = profile.incorporationDate
            .getOrElse(throw new IllegalStateException("Date of Incorporation data expected to be found in Incorporation"))

          getThresholdSummary() map {
            thresholdSummary => Ok(views.html.pages.threshold_summary(
              thresholdSummary,
              MonthYearModel.FORMAT_DD_MMMM_Y.format(dateOfIncorporation))
            )
          }
        }
  }

  def submit: Action[AnyContent] = authorised.async {
    implicit user =>
      implicit request => {
        withCurrentProfile { implicit profile =>
          getVatThresholdPostIncorp().map {
            case VatThresholdPostIncorp(true, _) =>
              save(VoluntaryRegistration(REGISTER_NO))
              //save(StartDateView(COMPANY_REGISTRATION_DATE)) //TODO: call S4LService to save StartDateView
              Redirect(vatRegFrontendService.buildVatRegFrontendUrlEntry)
            case _ => Redirect(controllers.routes.VoluntaryRegistrationController.show())
          }
        }
    }
  }

  def getThresholdSummary()(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[Summary] = {
    for {
      vatThresholdPostIncorp <- getVatThresholdPostIncorp()
    } yield thresholdToSummary(vatThresholdPostIncorp)
  }

  def thresholdToSummary(vatThresholdPostIncorp: VatThresholdPostIncorp): Summary = {
    Summary(Seq(
      SummaryVatThresholdBuilder(Some(vatThresholdPostIncorp)).section
    ))
  }

  def getVatThresholdPostIncorp()(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[VatThresholdPostIncorp] = {
    for {
      vatTradingDetails <- s4LService.fetchAndGet[S4LTradingDetails]()
      overThreshold <- vatTradingDetails.flatMap(_.overThreshold).pure
    } yield overThreshold.map(o => VatThresholdPostIncorp(o.selection, o.date)).get
  }
}
