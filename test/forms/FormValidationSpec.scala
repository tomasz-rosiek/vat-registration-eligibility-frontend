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

import forms.FormValidation.ErrorCode
import org.scalatest.{Inside, Inspectors}
import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.play.test.UnitSpec


class FormValidationSpec extends UnitSpec with Inside with Inspectors {

  "mandatoryText" must {

    val constraint = FormValidation.mandatoryText()("errorCode")

    "accept a non-blank string as Valid" in {
      constraint("non-blank string") shouldBe Valid
    }

    "reject blank string" in {
      forAll(Seq("", "  ", "    \t   "))(constraint(_) shouldBe Invalid("validation.errorCode.missing"))
    }
  }

  "Range validation" must {

    implicit val e:ErrorCode = "test"
    val constraint = FormValidation.inRange[Int](0, 100)
    val constraintWithErrorArgs = FormValidation.inRangeWithArgs[Int](0, 100)("Date Error")


    "accept values in range" in {
      forAll(Seq(0, 1, 2, 3, 50, 98, 99, 100))(constraint(_) shouldBe Valid)
    }

    "reject values below acceptable minimum" in {
      forAll(Seq(Int.MinValue, -10000, -1))(in => inside(constraint(in)) {
        case Invalid(err :: _) => err.message shouldBe "validation.test.range.below"
      })
    }

    "reject values above acceptable maximum" in {
      forAll(Seq(Int.MaxValue, 10000, 101))(in => inside(constraint(in)) {
        case Invalid(err :: _) => err.message shouldBe "validation.test.range.above"
      })
    }

    "reject values below acceptable minimum with custom error message" in {
      forAll(Seq(Int.MinValue, -10000, -1))(in => inside(constraintWithErrorArgs(in)) {
        case Invalid(err :: _) => err.message shouldBe "validation.test.range.below"
      })
    }

  }
}