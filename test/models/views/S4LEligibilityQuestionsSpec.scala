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

package models.views

import models.view.S4LEligibilityQuestions
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class S4LEligibilityQuestionsSpec extends UnitSpec {

  val testModelFullNOTVALID = S4LEligibilityQuestions(
    hasNino = Some(false),
    internationalBusiness = Some(true),
    moreThanOneBusinessOrLegalStatus = Some(true),
    agriculturalFRSOrAnnualAccount = Some(true),
    exceptionOrExemption = Some(true),
    racehorsesOrLandAndProperty = Some(true)
  )

  val testModelFullVALID = S4LEligibilityQuestions(
    hasNino = Some(true),
    internationalBusiness = Some(false),
    moreThanOneBusinessOrLegalStatus = Some(false),
    agriculturalFRSOrAnnualAccount = Some(false),
    exceptionOrExemption = Some(false),
    racehorsesOrLandAndProperty = Some(false)
  )

  val testModelNotFull = S4LEligibilityQuestions(
    hasNino = Some(true),
    internationalBusiness = Some(false),
    moreThanOneBusinessOrLegalStatus = None,
    agriculturalFRSOrAnnualAccount = None,
    exceptionOrExemption = None,
    racehorsesOrLandAndProperty = None
  )

  val testModelEmpty = S4LEligibilityQuestions(
    hasNino = None,
    internationalBusiness = None,
    moreThanOneBusinessOrLegalStatus = None,
    agriculturalFRSOrAnnualAccount = None,
    exceptionOrExemption = None,
    racehorsesOrLandAndProperty = None
  )

  val testJsonFullNOTVALID = Json.parse(
    """
      |{
      | "hasNino" : false,
      | "internationalBusiness" : true,
      | "moreThanOneBusinessOrLegalStatus" : true,
      | "agriculturalFRSOrAnnualAccount" : true,
      | "exceptionOrExemption" : true,
      | "racehorsesOrLandAndProperty" : true
      |}
    """.stripMargin
  )

  val testJsonFullVALID = Json.parse(
    """
      |{
      | "hasNino" : true,
      | "internationalBusiness" : false,
      | "moreThanOneBusinessOrLegalStatus" : false,
      | "agriculturalFRSOrAnnualAccount" : false,
      | "exceptionOrExemption" : false,
      | "racehorsesOrLandAndProperty" : false
      |}
    """.stripMargin
  )

  val testJsonNotFull = Json.parse(
    """
      |{
      | "hasNino" : true,
      | "internationalBusiness" : false
      |}
    """.stripMargin
  )

  val testJsonEmpty = Json.parse(
    """
      |{
      |
      |}
    """.stripMargin
  )

  "Writes" should {
    "return a set of json" when {
      "the model is full" in {
        val result = Json.toJson(testModelFullVALID)(S4LEligibilityQuestions.writes)
        result shouldBe testJsonFullVALID
      }

      "the model is partially full" in {
        val result = Json.toJson(testModelNotFull)(S4LEligibilityQuestions.writes)
        result shouldBe testJsonNotFull
      }

      "the model is empty" in {
        val result = Json.toJson(testModelEmpty)(S4LEligibilityQuestions.writes)
        result shouldBe testJsonEmpty
      }
    }
  }

  "BaseReads" should {
    "return a model" when {
      "the json is full" in {
        val result = Json.fromJson(testJsonFullVALID)(S4LEligibilityQuestions.baseReads)
        result shouldBe JsSuccess(testModelFullVALID)
      }

      "the json is partially full" in {
        val result = Json.fromJson(testJsonNotFull)(S4LEligibilityQuestions.baseReads)
        result shouldBe JsSuccess(testModelNotFull)
      }

      "the json is empty" in {
        val result = Json.fromJson(testJsonEmpty)(S4LEligibilityQuestions.baseReads)
        result shouldBe JsSuccess(testModelEmpty)
      }
    }
  }

  "isFull" should {
    "return true" when {
      "jsValue contains all values" in {
        val result = S4LEligibilityQuestions.isFull(testJsonFullVALID)
        result shouldBe true
      }

      "jsValue contains all values (not valid)" in {
        val result = S4LEligibilityQuestions.isFull(testJsonFullNOTVALID)
        result shouldBe true
      }
    }
    "return false" when {
      "jsValue does not contain any values" in {
        val result = S4LEligibilityQuestions.isFull(testJsonEmpty)
        result shouldBe false
      }
    }
    "return false" when {
      "jsValue contains some of the models values" in {
        val result = S4LEligibilityQuestions.isFull(testJsonNotFull)
        result shouldBe false
      }
    }
  }

  "isValid" should {
    "return true" when {
      "json hasNino as true and everything else false" in {
        val result = S4LEligibilityQuestions.isValid(testJsonFullVALID)
        result shouldBe "success"
      }
    }

    "return false" when {
      "json hasNino as false and everything else true" in {
        val result = S4LEligibilityQuestions.isValid(testJsonFullNOTVALID)
        result shouldBe "hasNino"
      }

      "the json is partially full" in {
        val result = S4LEligibilityQuestions.isValid(testJsonNotFull)
        result shouldBe "moreThanOneBusinessOrLegalStatus"
      }

      "the json is empty" in {
        val result = S4LEligibilityQuestions.isValid(testJsonEmpty)
        result shouldBe "hasNino"
      }
    }
  }
}
