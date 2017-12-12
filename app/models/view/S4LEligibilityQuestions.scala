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

import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class S4LEligibilityQuestions(hasNino: Option[Boolean],
                                   internationalBusiness: Option[Boolean],
                                   moreThanOneBusinessOrLegalStatus: Option[Boolean],
                                   agriculturalFRSOrAnnualAccount: Option[Boolean],
                                   exceptionOrExemption: Option[Boolean],
                                   racehorsesOrLandAndProperty: Option[Boolean])

object S4LEligibilityQuestions {
  val writes: Writes[S4LEligibilityQuestions] = (
    (__ \ "hasNino").writeNullable[Boolean] and
    (__ \ "internationalBusiness").writeNullable[Boolean] and
    (__ \ "moreThanOneBusinessOrLegalStatus").writeNullable[Boolean] and
    (__ \ "agriculturalFRSOrAnnualAccount").writeNullable[Boolean] and
    (__ \ "exceptionOrExemption").writeNullable[Boolean] and
    (__ \ "racehorsesOrLandAndProperty").writeNullable[Boolean]
  )(unlift(S4LEligibilityQuestions.unapply))

  val baseReads: Reads[S4LEligibilityQuestions] = (
    (__ \ "hasNino").readNullable[Boolean] and
    (__ \ "internationalBusiness").readNullable[Boolean] and
    (__ \ "moreThanOneBusinessOrLegalStatus").readNullable[Boolean] and
    (__ \ "agriculturalFRSOrAnnualAccount").readNullable[Boolean] and
    (__ \ "exceptionOrExemption").readNullable[Boolean] and
    (__ \ "racehorsesOrLandAndProperty").readNullable[Boolean]
  )(S4LEligibilityQuestions.apply _)

  val isFullReads: Reads[S4LEligibilityQuestions] = baseReads.filter(ValidationError("Not full"))(model =>
    model.hasNino.isDefined && model.internationalBusiness.isDefined &&
      model.moreThanOneBusinessOrLegalStatus.isDefined && model.agriculturalFRSOrAnnualAccount.isDefined &&
        model.exceptionOrExemption.isDefined && model.racehorsesOrLandAndProperty.isDefined
  )

  val isValidReads: Reads[S4LEligibilityQuestions] = (
    (__ \ "hasNino").read[Option[Boolean]](validateOptionBoolean(true)) and
    (__ \ "internationalBusiness").read[Option[Boolean]](validateOptionBoolean(false)) and
    (__ \ "moreThanOneBusinessOrLegalStatus").read[Option[Boolean]](validateOptionBoolean(false)) and
    (__ \ "agriculturalFRSOrAnnualAccount").read[Option[Boolean]](validateOptionBoolean(false)) and
    (__ \ "exceptionOrExemption").read[Option[Boolean]](validateOptionBoolean(false)) and
    (__ \ "racehorsesOrLandAndProperty").read[Option[Boolean]](validateOptionBoolean(false))
  )(S4LEligibilityQuestions.apply _)

  implicit val format: Format[S4LEligibilityQuestions] = Format(baseReads, writes)

  private def validateOptionBoolean(validCase: Boolean): Reads[Option[Boolean]] = new Reads[Option[Boolean]] {
    override def reads(json: JsValue): JsResult[Option[Boolean]] = {
      json.asOpt[Boolean].fold[JsResult[Option[Boolean]]](JsError(ValidationError("boolean is None")))(x =>
        if(x == validCase) JsSuccess(json.asOpt[Boolean]) else JsError(ValidationError(s"is not $validCase"))
      )
    }
  }

  def isFull(json: JsValue): Boolean  = json.validate(isFullReads).isSuccess

  def isValid(json: JsValue): String = {
    json.validate(isValidReads).fold(
      errors  => getFirstFailingQuestion(errors.map(q => q._1.toString.replace("/", "")).toList),
      success => "success"
    )
  }

  private def getFirstFailingQuestion(errorList: List[String], index: Int = 0): String = {
    val ordered = List("hasNino", "internationalBusiness", "moreThanOneBusinessOrLegalStatus", "agriculturalFRSOrAnnualAccount", "exceptionOrExemption", "racehorsesOrLandAndProperty")
    ordered.filter(x => errorList.contains(x)).head
  }
}
