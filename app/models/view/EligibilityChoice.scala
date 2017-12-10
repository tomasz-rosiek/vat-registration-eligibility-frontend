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
import play.api.libs.json.Json

sealed trait EligibilityChoice

case class ExpectationOverThresholdView(selection: Boolean, date: Option[LocalDate] = None) extends EligibilityChoice

object ExpectationOverThresholdView {
  implicit val format = Json.format[ExpectationOverThresholdView]
}

case class OverThresholdView(selection: Boolean, date: Option[LocalDate] = None) extends EligibilityChoice

object OverThresholdView {
  implicit val format = Json.format[OverThresholdView]
}

case class TaxableTurnover(yesNo: String) extends EligibilityChoice

object TaxableTurnover {
  val TAXABLE_YES = "TAXABLE_YES"
  val TAXABLE_NO  = "TAXABLE_NO"

  val valid = (item: String) => List(TAXABLE_YES, TAXABLE_NO).contains(item.toUpperCase)

  implicit val format = Json.format[TaxableTurnover]
}

case class VoluntaryRegistration(yesNo: String) extends EligibilityChoice

object VoluntaryRegistration {
  val REGISTER_YES  = "REGISTER_YES"
  val REGISTER_NO   = "REGISTER_NO"

  val valid = (item: String) => List(REGISTER_YES, REGISTER_NO).contains(item.toUpperCase)

  implicit val format = Json.format[VoluntaryRegistration]
}

case class VoluntaryRegistrationReason(reason: String) extends EligibilityChoice

object VoluntaryRegistrationReason {

  val SELLS           = "COMPANY_ALREADY_SELLS_TAXABLE_GOODS_OR_SERVICES"
  val INTENDS_TO_SELL = "COMPANY_INTENDS_TO_SELLS_TAXABLE_GOODS_OR_SERVICES_IN_THE_FUTURE"
  val NEITHER         = "NEITHER"

  val valid: (String) => Boolean = List(SELLS, INTENDS_TO_SELL, NEITHER).contains

  implicit val format = Json.format[VoluntaryRegistrationReason]
}