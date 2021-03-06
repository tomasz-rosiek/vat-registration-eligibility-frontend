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

package connectors

import javax.inject.{Inject, Singleton}

import config.WSHttp
import models.external.CompanyRegistrationProfile
import play.api.Logger
import play.api.libs.json.JsObject
import uk.gov.hmrc.play.config.ServicesConfig
import utils.VREFEFeatureSwitch

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, CoreGet, HeaderCarrier}

@Singleton
class CompanyRegistrationConnector @Inject()(val featureSwitch: VREFEFeatureSwitch) extends ServicesConfig {
  val companyRegistrationUrl: String = baseUrl("company-registration")
  val companyRegistrationUri: String = getConfString("company-registration.uri", "")
  lazy val stubUrl: String = baseUrl("incorporation-frontend-stubs")
  lazy val stubUri: String = getConfString("incorporation-frontend-stubs.uri","")
  val http: CoreGet = WSHttp

  def getCompanyRegistrationDetails(regId: String)(implicit hc : HeaderCarrier) : Future[CompanyRegistrationProfile] = {
    val url = if (useCompanyRegistration) s"$companyRegistrationUrl$companyRegistrationUri/corporation-tax-registration" else s"$stubUrl$stubUri"

    http.GET[JsObject](s"$url/$regId/corporation-tax-registration") map {
      response =>
        val status = (response \ "status").as[String]
        val txId = (response \ "confirmationReferences" \ "transaction-id").as[String]
        CompanyRegistrationProfile(status, txId)
    } recover {
      case badRequestErr: BadRequestException =>
        Logger.error(s"[CompanyRegistrationConnect] [getCompanyRegistrationDetails] - Received a BadRequest status code when expecting a Company Registration document for reg id: $regId")
        throw badRequestErr
      case ex: Exception =>
        Logger.error(s"[CompanyRegistrationConnect] [getCompanyRegistrationDetails] - Received an error when expecting a Company Registration document for reg id: $regId - error: ${ex.getMessage}")
        throw ex
    }
  }

  private[connectors] def useCompanyRegistration: Boolean = {
    featureSwitch.companyReg.enabled
  }
}



