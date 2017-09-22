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

package forms

import java.time.LocalDate

import models.MonthYearModel
import org.apache.commons.lang3.StringUtils
import play.api.Logger
import play.api.data.format.Formatter
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError, ValidationResult}
import play.api.data.{FieldMapping, FormError, Mapping}

import scala.util.Try

object FormValidation {
  type ErrorCode = String

  private def unconstrained[T] = Constraint[T] { (t: T) => Valid }

  def mandatoryText()(implicit e: ErrorCode): Constraint[String] = Constraint { input: String =>
    if (StringUtils.isNotBlank(input)) Valid else Invalid(s"validation.$e.missing")
  }

  def textMapping()(implicit e: ErrorCode): Mapping[String] = FieldMapping[String]()(stringFormat("missing")(Seq()))

  /* overrides Play's implicit stringFormatter and handles missing options (e.g. no radio button selected) */
  private def stringFormat(suffix: String)(args: Seq[Any] = Seq())(implicit e: ErrorCode): Formatter[String] = new Formatter[String] {

    def bind(key: String, data: Map[String, String]) = data.get(key).toRight(
      Seq(FormError(key, s"validation.$e.$suffix", args))
    )

    def unbind(key: String, value: String) = Map(key -> value)
  }

  private def booleanFormat()(args: Seq[Any] = Seq())(implicit e: ErrorCode): Formatter[Boolean] = new Formatter[Boolean] {
    def bind(key: String, data: Map[String, String]) = data.get(key).flatMap(input => Try(input.toBoolean).toOption)
      .toRight(Seq(FormError(key, s"validation.$e.missing", args)))

    def unbind(key: String, value: Boolean) = Map(key -> value.toString)
  }

  def missingBooleanFieldMappingArgs()(args: Seq[Any] = Seq())(implicit e: ErrorCode): Mapping[Boolean] = FieldMapping[Boolean]()(booleanFormat()(args))

  def missingBooleanFieldMapping()(implicit e: ErrorCode): Mapping[Boolean] =
    FieldMapping[Boolean]()(booleanFormat()(Seq()))

  def inRangeWithArgs[T](minValue: T, maxValue: T)(args: Seq[Any] = Seq())(implicit ordering: Ordering[T], e: ErrorCode): Constraint[T] =
    Constraint[T] { (t: T) =>
      Logger.info(s"Checking constraint for value $t in the range of [$minValue, $maxValue]")
      (ordering.compare(t, minValue).signum, ordering.compare(t, maxValue).signum) match {
        case (1, -1) | (0, _) | (_, 0) => Valid
        case (_, 1) => Invalid(ValidationError(s"validation.$e.range.above", maxValue))
        case (-1, _) if !args.isEmpty  => Invalid(ValidationError(s"validation.$e.range.below", args.head))
        case (-1, _) => Invalid(ValidationError(s"validation.$e.range.below", minValue))
      }
    }

  object Dates {
    def nonEmptyMonthYearModel(constraint: => Constraint[MonthYearModel] = unconstrained)(implicit e: ErrorCode): Constraint[MonthYearModel] =
      Constraint { pdm =>
        mandatoryText.apply(Seq(pdm.month, pdm.year).mkString.trim) match {
          case Valid => constraint(pdm)
          case err@_ => err
        }
      }
    def validPartialMonthYearModel(dateConstraint: => Constraint[LocalDate] = unconstrained)(implicit e: ErrorCode): Constraint[MonthYearModel] =
      Constraint(dm => dm.toLocalDate.fold[ValidationResult](Invalid(s"validation.$e.invalid"))(dateConstraint(_)))

  }
}
