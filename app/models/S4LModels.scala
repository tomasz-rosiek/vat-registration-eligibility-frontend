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

package models

import models.api.{VatChoice, VatScheme, VatServiceEligibility, VatThresholdPostIncorp, VatTradingDetails}
import play.api.libs.json.{Json, OFormat}
import common.ErrorUtil.fail
import models.view.{OverThresholdView, TaxableTurnover, VoluntaryRegistration, VoluntaryRegistrationReason}
import models.view.VoluntaryRegistration.REGISTER_YES
import models.api.VatChoice.{NECESSITY_OBLIGATORY, NECESSITY_VOLUNTARY}

trait S4LModelTransformer[C] {
  // Returns an S4L container for a logical group given a VatScheme
  def toS4LModel(vatScheme: VatScheme): C
}

trait S4LApiTransformer[C, API] {
  // Returns logical group API model given an S4L container
  def toApi(container: C): API
}

case class S4LVatEligibility
(
  vatEligibility: Option[VatServiceEligibility] = None
)

object S4LVatEligibility {
  implicit val format: OFormat[S4LVatEligibility] = Json.format[S4LVatEligibility]

  implicit val modelT = new S4LModelTransformer[S4LVatEligibility] {
    override def toS4LModel(vs: VatScheme): S4LVatEligibility =
      S4LVatEligibility(vatEligibility = ApiModelTransformer[VatServiceEligibility].toViewModel(vs))
  }

  def error = throw fail("VatServiceEligibility")

  implicit val apiT = new S4LApiTransformer[S4LVatEligibility, VatServiceEligibility] {
    override def toApi(c: S4LVatEligibility): VatServiceEligibility = {
      c.vatEligibility.map(ve => VatServiceEligibility(
        haveNino = ve.haveNino,
        doingBusinessAbroad = ve.doingBusinessAbroad,
        doAnyApplyToYou = ve.doAnyApplyToYou,
        applyingForAnyOf = ve.applyingForAnyOf,
        companyWillDoAnyOf = ve.companyWillDoAnyOf)
      ).getOrElse(error)
    }
  }
}

case class S4LTradingDetails(taxableTurnover: Option[TaxableTurnover] = None,
                             voluntaryRegistration: Option[VoluntaryRegistration] = None,
                             voluntaryRegistrationReason: Option[VoluntaryRegistrationReason] = None,
                             overThreshold: Option[OverThresholdView] = None)

object S4LTradingDetails {
  implicit val format: OFormat[S4LTradingDetails] = Json.format[S4LTradingDetails]
  implicit val tradingDetails: S4LKey[S4LTradingDetails] = S4LKey("VatTradingDetails")

  implicit val modelT = new S4LModelTransformer[S4LTradingDetails] {
    // map VatScheme to VatTradingDetails
    override def toS4LModel(vs: VatScheme): S4LTradingDetails =
      S4LTradingDetails(
        taxableTurnover = ApiModelTransformer[TaxableTurnover].toViewModel(vs),
        overThreshold = ApiModelTransformer[OverThresholdView].toViewModel(vs)
      )
  }

  def error = throw fail("VatTradingDetails")

  implicit val apiT = new S4LApiTransformer[S4LTradingDetails, VatTradingDetails] {
    // map S4LTradingDetails to VatTradingDetails
    override def toApi(c: S4LTradingDetails): VatTradingDetails =
      VatTradingDetails(
        VatChoice(
          necessity = c.voluntaryRegistration.map(vr =>
            if (vr.yesNo == REGISTER_YES) NECESSITY_VOLUNTARY else NECESSITY_OBLIGATORY).getOrElse(NECESSITY_OBLIGATORY),
          reason = c.voluntaryRegistrationReason.map(_.reason),
          vatThresholdPostIncorp = c.overThreshold.map(vtp =>
            VatThresholdPostIncorp(
              overThresholdSelection = vtp.selection,
              overThresholdDate = vtp.date
            )
          )
        )
      )
  }
}