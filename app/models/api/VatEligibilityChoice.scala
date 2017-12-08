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

package models.api

import org.joda.time.LocalDate
import play.api.libs.json.{Json, OFormat}

case class VatEligibilityChoice(necessity: String, // "obligatory" or "voluntary"
                                reason: Option[String] = None,
                                vatThresholdPostIncorp: Option[VatThresholdPostIncorp] = None,
                                vatExpectedThresholdPostIncorp:Option[VatExpectedThresholdPostIncorp] = None)

object VatEligibilityChoice {

  val NECESSITY_OBLIGATORY = "obligatory"
  val NECESSITY_VOLUNTARY = "voluntary"

  implicit val format: OFormat[VatEligibilityChoice] = Json.format[VatEligibilityChoice]

}

case class Threshold(overThresholdDate: Option[LocalDate],
                     expectedOverThresholdDate:Option[LocalDate],
                     mandatoryRegistration:Boolean,
                     voluntaryReason: Option[String])
object Threshold{
  implicit val format = Json.format[Threshold]
}
