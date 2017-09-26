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

package utils

import java.time.LocalDate

import common.enums.{CacheKeys, VatRegStatus}
import helpers.VatRegSpec
import models.CurrentProfile
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.CurrentProfileService
import org.mockito.Matchers
import org.mockito.Mockito.when
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future


class SessionProfileSpec extends VatRegSpec {

  object TestSession extends SessionProfile {
    override val currentProfileService: CurrentProfileService = mockCurrentProfileService
  }

  def testFunc : Future[Result] = Future.successful(Ok)
  implicit val request = FakeRequest()

  def validProfile(incorpDate: Option[LocalDate] = None) = CurrentProfile("TEST LTD", "regId", "txId", VatRegStatus.DRAFT, incorpDate)

  "calling withCurrentProfile" should {
    "carry out the passed function" in {
      when(mockCurrentProfileService.getCurrentProfile()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(validProfile()))

      val result = await(TestSession.withCurrentProfile { _ => testFunc })
      result.header.status mustBe OK
    }
  }
}
