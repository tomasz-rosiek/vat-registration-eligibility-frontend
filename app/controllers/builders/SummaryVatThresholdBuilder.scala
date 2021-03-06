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

import java.time.format.DateTimeFormatter

import models.api.{VatExpectedThresholdPostIncorp, VatThresholdPostIncorp}
import models.view.{SummaryRow, SummarySection}

case class SummaryVatThresholdBuilder(vatThresholdPostIncorp: Option[VatThresholdPostIncorp] = None,
                                      vatExpectedThresholdPostIncorp: Option[VatExpectedThresholdPostIncorp] = None)
  extends SummarySectionBuilder {

  override val sectionId: String = "threshold"
  val monthYearPresentationFormatter = DateTimeFormatter.ofPattern("MMMM y")

  val overThresholdSelectionRow: SummaryRow = SummaryRow(
    s"$sectionId.overThresholdSelection",
    vatThresholdPostIncorp.fold("")(x => if(x.overThresholdSelection) "app.common.yes" else "app.common.no"),
    Some(controllers.routes.ThresholdController.goneOverShow())
  )

  val overThresholdDateRow: SummaryRow = SummaryRow(
    s"$sectionId.overThresholdDate",
    vatThresholdPostIncorp.map(_.overThresholdDate.fold("")(_.format(monthYearPresentationFormatter))).getOrElse(""),
    Some(controllers.routes.ThresholdController.goneOverShow())
  )

  val dayMonthYearPresentationFormatter = DateTimeFormatter.ofPattern("dd MMMM y")

  val expectedOverThresholdSelectionRow: SummaryRow = SummaryRow(
    s"$sectionId.expectationOverThresholdSelection",
    vatExpectedThresholdPostIncorp.fold("")(x => if(x.expectedOverThresholdSelection) "app.common.yes" else "app.common.no"),
    Some(controllers.routes.ThresholdController.expectationOverShow())
  )

  val expectedOverThresholdDateRow: SummaryRow = SummaryRow(
    s"$sectionId.expectationOverThresholdDate",
    vatExpectedThresholdPostIncorp.map(_.expectedOverThresholdDate.fold("")(_.format(dayMonthYearPresentationFormatter))).getOrElse(""),
    Some(controllers.routes.ThresholdController.expectationOverShow())
  )

  val section: SummarySection = SummarySection(
    sectionId,
    rows = Seq(
      (overThresholdSelectionRow, true),
      (overThresholdDateRow, vatThresholdPostIncorp.flatMap(_.overThresholdDate).isDefined),
      (expectedOverThresholdSelectionRow, true),
      (expectedOverThresholdDateRow, vatExpectedThresholdPostIncorp.flatMap(_.expectedOverThresholdDate).isDefined)
    )
  )
}
