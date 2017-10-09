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

import fixtures.VatRegistrationFixture
import models.api.{VatEligibilityChoice, VatScheme}
import models.view.TaxableTurnover
import models.view.TaxableTurnover._
import models.{ApiModelTransformer, S4LVatEligibilityChoice}
import uk.gov.hmrc.play.test.UnitSpec

class TaxableTurnoverSpec extends UnitSpec with VatRegistrationFixture {

  "apply" should {
    "convert a VatChoice (Obligatory) to view model" in {
      val vatSchemeObligatory = vatScheme(vatServiceEligibility = Some(vatServiceEligibility(necessity = VatEligibilityChoice.NECESSITY_OBLIGATORY)))
      ApiModelTransformer[TaxableTurnover].toViewModel(vatSchemeObligatory) shouldBe Some(TaxableTurnover(TAXABLE_YES))
    }

    "convert a VatChoice (Voluntary) to view model" in {
      val vatSchemeVoluntary = vatScheme(vatServiceEligibility = Some(vatServiceEligibility(necessity = VatEligibilityChoice.NECESSITY_VOLUNTARY)))
      ApiModelTransformer[TaxableTurnover].toViewModel(vatSchemeVoluntary) shouldBe Some(TaxableTurnover(TAXABLE_NO))
    }

    "convert a none VatChoice to empty view model" in {
      val vatSchemeVoluntary = VatScheme(testRegId)
      ApiModelTransformer[TaxableTurnover].toViewModel(vatSchemeVoluntary) shouldBe None
    }
  }

  "ViewModelFormat" should {
    val s4LVatChoice: S4LVatEligibilityChoice = S4LVatEligibilityChoice(taxableTurnover = Some(validTaxableTurnover))

    "extract taxableTurnover from vatChoice" in {
      TaxableTurnover.viewModelFormat.read(s4LVatChoice) shouldBe Some(validTaxableTurnover)
    }

    "update empty vatContact with taxableTurnover" in {
      TaxableTurnover.viewModelFormat.update(validTaxableTurnover, Option.empty[S4LVatEligibilityChoice]).taxableTurnover shouldBe Some(validTaxableTurnover)
    }

    "update non-empty vatContact with taxableTurnover" in {
      TaxableTurnover.viewModelFormat.update(validTaxableTurnover, Some(s4LVatChoice)).taxableTurnover shouldBe Some(validTaxableTurnover)
    }

  }

}
