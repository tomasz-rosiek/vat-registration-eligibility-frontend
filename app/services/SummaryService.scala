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

package services

import javax.inject.Inject

import controllers.builders.{SummaryResourceBuilder, _}
import models.CurrentProfile
import models.api.VatServiceEligibility
import models.view.Summary
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class SummaryServiceImpl @Inject()(val eligibilityService: EligibilityService) extends SummaryService

trait SummaryService {
  val eligibilityService: EligibilityService

  def getEligibilitySummary(implicit hc: HeaderCarrier, profile: CurrentProfile): Future[Summary] = {
    eligibilityService.getEligibility.map(a => eligibilitySummary(eligibilityService.toApi(a)))
  }

  private def eligibilitySummary(vs: VatServiceEligibility): Summary = {
    Summary(Seq(
      SummaryNationalInsuranceBuilder(vs).section,
      SummaryInternationalBusinessBuilder(vs).section,
      SummaryOtherBusinessBuilder(vs).section,
      SummaryOtherVatSchemeBuilder(vs).section,
      SummaryVatExemptionBuilder(vs).section,
      SummaryResourceBuilder(vs).section
    ))
  }
}