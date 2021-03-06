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
import models.external.BusinessProfile
import play.api.Logger
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{BadRequestException, CoreGet, HeaderCarrier, HttpReads, NotFoundException, Upstream4xxResponse, Upstream5xxResponse}

@Singleton
class BusinessRegistrationConnector @Inject()() extends ServicesConfig {
  val businessRegUrl = baseUrl("business-registration")
  val http: CoreGet = WSHttp

  def retrieveBusinessProfile(implicit hc: HeaderCarrier, rds: HttpReads[BusinessProfile]): Future[BusinessProfile] = {
    http.GET[BusinessProfile](s"$businessRegUrl/business-registration/business-tax-registration") map {
      profile =>
        profile
    } recover {
      case e =>
        throw logResponse(e, "retrieveBusinessProfile", "retrieving business profile")
    }
  }

  private[connectors] def logResponse(e: Throwable, f: String, m: String, regId: Option[String] = None): Throwable = {
    val optRegId = regId.map(r => s" and regId: $regId").getOrElse("")
    def log(s: String) = Logger.error(s"[BusinessRegistrationConnector] [$f] received $s when $m$optRegId")
    e match {
      case e: NotFoundException => log("NOT FOUND")
      case e: BadRequestException => log("BAD REQUEST")
      case e: Upstream4xxResponse => e.upstreamResponseCode match {
      case 403 => log("FORBIDDEN")
      case _ => log(s"Upstream 4xx: ${e.upstreamResponseCode} ${e.message}")
    }
      case e: Upstream5xxResponse => log(s"Upstream 5xx: ${e.upstreamResponseCode}")
      case e: Exception => log(s"ERROR: ${e.getMessage}")
    }
    e
  }
}
