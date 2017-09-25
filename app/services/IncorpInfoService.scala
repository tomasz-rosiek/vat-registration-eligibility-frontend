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

package services

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import cats.data.OptionT
import cats.instances.future._
import connectors.{IncorporationInformationConnector, KeystoreConnector, OptionalResponse}
import models.external.IncorporationInfo
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class IncorpInfoService @Inject()(val keystoreConnector: KeystoreConnector,
                                  val iiConnector: IncorporationInformationConnector) {

  def fetchDateOfIncorporation()(implicit hc: HeaderCarrier): Future[LocalDate] = {
    OptionT(keystoreConnector.fetchAndGet[IncorporationInfo]("incorporationStatus"))
      .subflatMap(_.statusEvent.incorporationDate)
      .getOrElse(throw new IllegalStateException("Date of Incorporation data expected to be found in Incorporation"))
  }

  def fetchIncorporationInfo()(implicit headerCarrier: HeaderCarrier) =
    OptionT(keystoreConnector.fetchAndGet[IncorporationInfo]("incorporationStatus"))

  def getCompanyName(regId: String, txId: String)(implicit hc: HeaderCarrier): Future[String] = {
    iiConnector.getCompanyName(regId, txId) map(_.\("company_name").as[String])
  }
}
