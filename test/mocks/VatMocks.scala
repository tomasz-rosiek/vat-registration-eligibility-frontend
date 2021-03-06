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

package mocks

import connectors.{CompanyRegistrationConnector, IncorporationInformationConnector, VatRegistrationConnector}
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import services._
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

trait VatMocks
  extends SaveForLaterMock
    with KeystoreMock
    with WSHTTPMock {

  this: MockitoSugar =>
  implicit lazy val mockMessagesApi = mock[MessagesApi]
  implicit lazy val mockAuthConnector = mock[AuthConnector]
  implicit lazy val mockSessionCache = mock[SessionCache]
  implicit lazy val mockAudit = mock[Audit]
  implicit lazy val mockS4LService = mock[S4LService]
  implicit lazy val mockCurrentProfileService = mock[CurrentProfileService]
  implicit lazy val mockRegConnector = mock[VatRegistrationConnector]
  implicit lazy val mockCompanyRegConnector = mock[CompanyRegistrationConnector]
  implicit lazy val mockIIConnector = mock[IncorporationInformationConnector]
  implicit lazy val mockVatRegistrationService = mock[VatRegistrationService]
  implicit lazy val mockIncorpInfoService = mock[IncorpInfoService]
  implicit lazy val mockVatRegFrontendService = mock[VatRegFrontendService]
  implicit lazy val mockSummaryService = mock[SummaryService]
  implicit lazy val mockEligibilityService = mock[EligibilityService]
  implicit lazy val mockCancellationService = mock[CancellationService]
}
