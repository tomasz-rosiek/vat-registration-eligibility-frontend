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

import common.enums.VatRegStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._


case class VatScheme(id: String,
                     vatServiceEligibility: Option[VatServiceEligibility] = None,
                      status: VatRegStatus.Value
                    )

object VatScheme {
  implicit val format: OFormat[VatScheme] = (
    (__ \ "registrationId").format[String] and
    (__ \ "vatEligibility").formatNullable[VatServiceEligibility] and
      (__ \ "status").format[VatRegStatus.Value]
  )(VatScheme.apply, unlift(VatScheme.unapply))
}
