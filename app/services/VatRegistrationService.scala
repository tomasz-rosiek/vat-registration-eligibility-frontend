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
import cats.implicits._
import common.ErrorUtil.fail
import connectors.{KeystoreConnector, VatRegistrationConnector}
import models._
import models.api._
import models.external.IncorporationInfo
import play.api.libs.json.Format
import uk.gov.hmrc.play.http.HeaderCarrier
import common.enums.CacheKeys.{CurrentProfile => CurrentProfileKey}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class VatRegistrationService @Inject()(val s4LService: S4LService,
                                       val keystoreConnector: KeystoreConnector,
                                       val vatRegConnector: VatRegistrationConnector) {

  private[services] def s4l[T: Format : S4LKey]()(implicit hc: HeaderCarrier, profile: CurrentProfile) =
    s4LService.fetchAndGet[T]()

  def getVatScheme()(implicit profile: CurrentProfile, hc: HeaderCarrier): Future[VatScheme] =
    vatRegConnector.getRegistration(profile.registrationId)

  def deleteVatScheme()(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[Unit] =
    vatRegConnector.deleteVatScheme(profile.registrationId)

  def submitVatEligibility()(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[VatServiceEligibility] = {
    def merge(fresh: Option[S4LVatEligibility], vs: VatScheme): VatServiceEligibility =
      fresh.fold(
        vs.vatServiceEligibility.getOrElse(throw fail("VatServiceEligibility"))
      )(s4l => S4LVatEligibility.apiT.toApi(s4l))

    for {
      vs       <- getVatScheme()
      ve       <- s4l[S4LVatEligibility]()
      response <- vatRegConnector.upsertVatEligibility(profile.registrationId, merge(ve, vs))
    } yield response
  }

  def getIncorporationInfo(txId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[IncorporationInfo]] =
    vatRegConnector.getIncorporationInfo(txId).value

  def getIncorporationDate(txId: String)(implicit headerCarrier: HeaderCarrier): Future[Option[LocalDate]] =
    keystoreConnector.fetchAndGet[CurrentProfile](CurrentProfileKey.toString) flatMap { profile =>
      profile
        .getOrElse(throw new IllegalStateException("Current Profile expected to be found"))
        .incorporationDate match {
        case None => for {
          incorpDate <- OptionT(getIncorporationInfo(txId)).subflatMap(_.statusEvent.incorporationDate).value
          _ <- keystoreConnector.cache[CurrentProfile](CurrentProfileKey.toString, profile.get.copy(incorporationDate = incorpDate))
        } yield incorpDate
        case o@_ => Future.successful(o)
      }
    }
}
