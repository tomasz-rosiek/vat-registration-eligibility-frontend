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

import java.time.LocalDate

import cats.data.OptionT
import common.enums.VatRegStatus
import connectors.BusinessRegistrationConnector
import helpers.VatRegSpec
import models.CurrentProfile
import models.external.{BusinessProfile, CompanyRegistrationProfile, IncorporationInfo}
import org.mockito.Matchers
import org.mockito.Mockito.when
import play.api.libs.json.Format
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpReads}

import scala.concurrent.Future

class CurrentProfileServiceSpec extends VatRegSpec {
  val mockBusinessRegistrationConnector = mock[BusinessRegistrationConnector]

  val testService = new CurrentProfileService(mockKeystoreConnector,
                                              mockBusinessRegistrationConnector,
                                              mockCompanyRegConnector,
                                              mockIncorpInfoService,
                                              mockVatRegistrationService)

  val now = LocalDate.now()

  val testCompanyName = "testCompanyName"
  val regId = "12345"
  val txId = "000-12345"

  def testCurrentProfile(incorpDate: Option[LocalDate] = Some(now)) = CurrentProfile(
    companyName           = testCompanyName,
    registrationId        = regId,
    transactionId         = txId,
    vatRegistrationStatus = VatRegStatus.DRAFT,
    incorporationDate     = incorpDate
  )

  "getCurrentProfile" should {
    implicit val hc = HeaderCarrier()

    "return a CurrentProfile" when {
      "fetched from Keystore" in {
        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any[HeaderCarrier](), Matchers.any[Format[CurrentProfile]]()))
          .thenReturn(Future.successful(Some(testCurrentProfile())))

        val result = await(testService.getCurrentProfile())
        result mustBe testCurrentProfile()
      }

      "build and store in Keystore" in {
        val businessProfile = BusinessProfile(regId, "EN")
        val compRegDetails = CompanyRegistrationProfile("accepted", txId)

        when(mockKeystoreConnector.fetchAndGet[CurrentProfile](Matchers.any())(Matchers.any[HeaderCarrier](), Matchers.any[Format[CurrentProfile]]()))
          .thenReturn(Future.successful(None))

        when(mockBusinessRegistrationConnector.retrieveBusinessProfile(Matchers.any[HeaderCarrier](), Matchers.any[HttpReads[BusinessProfile]]))
          .thenReturn(Future.successful(businessProfile))

        when(mockCompanyRegConnector.getCompanyRegistrationDetails(Matchers.any())(Matchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(compRegDetails))

        when(mockIncorpInfoService.getCompanyName(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(testCompanyName))

        when(mockVatRegistrationService.getIncorporationInfo(Matchers.any())(Matchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(Some(testIncorporationInfo)))

        when(mockKeystoreConnector.cache[CurrentProfile](Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any()))
          .thenReturn(Future.successful(CacheMap("", Map())))

        val result = await(testService.getCurrentProfile())
        result mustBe testCurrentProfile(testIncorporationInfo.statusEvent.incorporationDate)
      }
    }
  }
}
