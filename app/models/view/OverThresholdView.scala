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

package models.view

import java.time.LocalDate

import models.{ApiModelTransformer, MonthYearModel, S4LTradingDetails, ViewModelFormat}
import models.api.{VatScheme, VatThresholdPostIncorp}
import play.api.libs.json.Json

import scala.util.Try

case class OverThresholdView(selection: Boolean, date: Option[LocalDate] = None)

object OverThresholdView {

  def bind(selection: Boolean, dateModel: Option[MonthYearModel]): OverThresholdView =
    OverThresholdView(selection, dateModel.flatMap(_.toLocalDate))

  def unbind(overThreshold: OverThresholdView): Option[(Boolean, Option[MonthYearModel])] =
    Try {
      overThreshold.date.fold((overThreshold.selection, Option.empty[MonthYearModel])) {
        d => (overThreshold.selection, Some(MonthYearModel.fromLocalDate(d)))
      }
    }.toOption

  implicit val format = Json.format[OverThresholdView]

  implicit val viewModelFormat = ViewModelFormat(
    readF = (group: S4LTradingDetails) => group.overThreshold,
    updateF = (c: OverThresholdView, g: Option[S4LTradingDetails]) =>
      g.getOrElse(S4LTradingDetails()).copy(overThreshold = Some(c))
  )

  // Returns a view model for a specific part of a given VatScheme API model
  implicit val modelTransformer = ApiModelTransformer[OverThresholdView] { vs: VatScheme =>
    vs.tradingDetails.map(_.vatChoice.vatThresholdPostIncorp).collect {
      case Some(VatThresholdPostIncorp(selection, d@_)) => OverThresholdView(selection, d)
    }
  }
}