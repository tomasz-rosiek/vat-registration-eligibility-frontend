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

package helpers

import java.time.LocalDate

import builders.AuthBuilder
import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.enums.VatRegStatus
import fixtures.{LoginFixture, VatRegistrationFixture}
import mocks.VatMocks
import models.CurrentProfile
import org.mockito.Mockito.reset
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Assertion, BeforeAndAfterEach, Inside, Inspectors}
import play.api.i18n.MessagesApi
import play.api.inject.Injector
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class VatRegSpec extends UnitSpec with WithFakeApplication
  with MockitoSugar with VatMocks with LoginFixture with Inside with Inspectors
  with ScalaFutures with ApplicativeSyntax with FutureInstances with BeforeAndAfterEach with FutureAssertions with VatRegistrationFixture {

  val injector : Injector = fakeApplication.injector

  implicit lazy val mockMessages = injector.instanceOf[MessagesApi]

  implicit val hc = HeaderCarrier()

  implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1",
    VatRegStatus.draft,Some(LocalDate.of(2016, 12, 21)))

  override implicit val patienceConfig = PatienceConfig(timeout = Span(1, Seconds), interval = Span(50, Millis))

  override def beforeEach() {
    reset(mockVatRegistrationService)
    reset(mockS4LConnector)
    reset(mockS4LService)
    reset(mockKeystoreConnector)
    reset(mockAuthConnector)
    reset(mockSessionCache)
    reset(mockAudit)
    reset(mockVatRegistrationService)
    reset(mockRegConnector)
    reset(mockCompanyRegConnector)
    reset(mockIIConnector)
    reset(mockIncorpInfoService)
    reset(mockWSHttp)
    reset(mockCurrentProfileService)
    reset(mockVatRegFrontendService)
    reset(mockCancellationService)
  }

  // Placeholder for custom configuration
  // Use this if you want to configure the app
  // implicit override lazy val app: Application = new GuiceApplicationBuilder().configure().build()


  def submitAuthorised(a: => Action[AnyContent], r: => FakeRequest[AnyContentAsFormUrlEncoded])
                      (test: Future[Result] => Assertion)
                      (implicit mockAuthConnector: AuthConnector): Unit =
    AuthBuilder.submitWithAuthorisedUser(a, r)(test)

  def callAuthorised(a: Action[AnyContent])(test: Future[Result] => Assertion): Unit =
    AuthBuilder.withAuthorisedUser(a)(test)

}
