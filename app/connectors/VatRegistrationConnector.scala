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

import javax.inject.Inject

import common.enums.VatRegStatus
import config.WSHttp
import models.api._
import models.external.IncorporationInfo
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class VatRegistrationConnectorImpl @Inject()(val http: WSHttp, config: ServicesConfig) extends VatRegistrationConnector {
  val vatRegUrl = config.baseUrl("vat-registration")
}

trait VatRegistrationConnector {

  val vatRegUrl: String
  val http: WSHttp

  def getRegistration(regId: String)(implicit hc: HeaderCarrier, rds: HttpReads[VatScheme]): Future[VatScheme] = {
    http.GET[VatScheme](s"$vatRegUrl/vatreg/$regId/get-scheme").recover{
      case e: Exception => throw logResponse(e, "getRegistration")
    }
  }

  def upsertVatEligibility(regId: String,
                           vatServiceEligibility: VatServiceEligibility)(implicit hc: HeaderCarrier, rds: HttpReads[VatServiceEligibility]): Future[VatServiceEligibility] = {
    http.PATCH[VatServiceEligibility, VatServiceEligibility](s"$vatRegUrl/vatreg/$regId/service-eligibility", vatServiceEligibility).recover{
      case e: Exception => throw logResponse(e, "upsertVatEligibility")
    }
  }

  def getIncorporationInfo(transactionId: String)(implicit hc: HeaderCarrier): Future[Option[IncorporationInfo]] = {
    http.GET[IncorporationInfo](s"$vatRegUrl/vatreg/incorporation-information/$transactionId").map(Some(_)).recover {
      case _ => None
    }
  }

  def getStatus(regId: String)(implicit hc: HeaderCarrier, rds: HttpReads[VatScheme]): Future[VatRegStatus.Value] = {
    http.GET[JsObject](s"$vatRegUrl/vatreg/$regId/status") map { json =>
      (json \ "status").as[VatRegStatus.Value]
    } recover {
      case e: Exception => throw logResponse(e, "getStatus")
    }
  }
}

