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

package controllers.builders

import models.api.VatServiceEligibility
import models.view.{SummaryRow, SummarySection}

case class SummaryNationalInsuranceBuilder(vatServiceEligibility: VatServiceEligibility) extends SummarySectionBuilder {

  override val sectionId: String = "nationalInsurance"

  val nationalInsuranceRow: SummaryRow = SummaryRow(
    s"$sectionId.hasNino",
    booleanToMessageKey(vatServiceEligibility.haveNino),
    Some(controllers.routes.EligibilityController.showHaveNino())
  )

  val section: SummarySection = SummarySection(
    sectionId,
    rows = Seq(
      (nationalInsuranceRow, true)
    )
  )
}
