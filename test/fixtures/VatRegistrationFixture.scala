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

package fixtures

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import models.api._
import models.external.{IncorporationInfo, _}
import play.api.http.Status._
import uk.gov.hmrc.play.http._

trait VatRegistrationFixture extends TradingDetailsFixture {

  //Test variables
  val testRegId = "VAT123456"

  //Api models
  val validServiceEligibility = VatServiceEligibility(Some(true), Some(false), Some(false), Some(false), Some(false))

  val emptyVatScheme = VatScheme(testRegId)

  val validVatScheme = VatScheme(
    id = testRegId,
    tradingDetails = Some(validVatTradingDetails)
  )

  val testIncorporationInfo = IncorporationInfo(
    IncorpSubscription(
      transactionId = "000-434-23",
      regime = "vat",
      subscriber = "scrs",
      callbackUrl = "http://localhost:9896/TODO-CHANGE-THIS"),
    IncorpStatusEvent(
      status = "accepted",
      crn = Some("90000001"),
      incorporationDate = Some(LocalDate.of(2016, 8, 5)),
      description = Some("Some description")))

  def vatScheme(id: String = testRegId, vatTradingDetails: Option[VatTradingDetails] = None): VatScheme =
    VatScheme(
      id = id,
      tradingDetails = vatTradingDetails
    )

}
