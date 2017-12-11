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

package connectors

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import models.external.IncorporationInfo
import play.api.http.Status.{BAD_GATEWAY, FORBIDDEN, NOT_FOUND, OK}
import config.WSHttp

import scala.language.postfixOps
import uk.gov.hmrc.http.{ HttpResponse, InternalServerException, NotFoundException, Upstream4xxResponse }

class VatRegistrationConnectorSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val connector = new VatRegistrationConnector {
      override val vatRegUrl: String = "tst-url"
      override val http: WSHttp = mockWSHttp
    }
  }

  val forbidden = Upstream4xxResponse(FORBIDDEN.toString, FORBIDDEN, FORBIDDEN)
  val notFound = new NotFoundException(NOT_FOUND.toString)
  val internalServiceException = new InternalServerException(BAD_GATEWAY.toString)

  "Calling getRegistration" should {
    "return the correct VatResponse when the microservice returns a Vat Registration model" in new Setup {
      mockHttpGET[VatScheme]("tst-url", validVatScheme)
      connector.getRegistration("tstID") returns validVatScheme
    }
    "return the correct VatResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedGET[VatScheme]("tst-url", forbidden)
      connector.getRegistration("tstID") failedWith forbidden
    }
    "return the correct VatResponse when a Not Found response is returned by the microservice" in new Setup {
      mockHttpFailedGET[VatScheme]("tst-url", notFound)
      connector.getRegistration("not_found_tstID") failedWith notFound
    }
    "return the correct VatResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[VatScheme]("test-url", internalServiceException)
      connector.getRegistration("tstID") failedWith internalServiceException
    }
  }

  "Calling upsertVatEligibility" should {

    val vatEligibility = validServiceEligibility

    "return the correct VatResponse when the microservice completes and returns a VatServiceEligibility model" in new Setup {
      mockHttpPATCH[VatServiceEligibility, VatServiceEligibility]("tst-url", vatEligibility)
      connector.upsertVatEligibility("tstID", vatEligibility) returns vatEligibility
    }
    "return the correct VatResponse when a Forbidden response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[VatServiceEligibility, VatServiceEligibility]("tst-url", forbidden)
      connector.upsertVatEligibility("tstID", vatEligibility) failedWith forbidden
    }
    "return a Not Found VatResponse when the microservice returns a NotFound response (No VatRegistration in database)" in new Setup {
      mockHttpFailedPATCH[VatServiceEligibility, VatServiceEligibility]("tst-url", notFound)
      connector.upsertVatEligibility("tstID", vatEligibility) failedWith notFound
    }
    "return the correct VatResponse when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedPATCH[VatServiceEligibility, VatServiceEligibility]("tst-url", internalServiceException)
      connector.upsertVatEligibility("tstID", vatEligibility) failedWith internalServiceException
    }
  }

  "Calling getIncorporationInfo" should {

    "return a IncorporationInfo when it can be retrieved from the microservice" in new Setup {
      mockHttpGET[IncorporationInfo]("tst-url", testIncorporationInfo)
      await(connector.getIncorporationInfo("tstID")) shouldBe Some(testIncorporationInfo)
    }

    "fail when an Internal Server Error response is returned by the microservice" in new Setup {
      mockHttpFailedGET[IncorporationInfo]("test-url", notFound)
      await(connector.getIncorporationInfo("tstID")) shouldBe None
    }
  }
}
