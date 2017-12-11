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

import common.enums.{CacheKeys, EligibilityQuestions, VatRegStatus}
import fixtures.{S4LFixture, VatRegistrationFixture}
import helpers.FutureAssertions
import mocks.VatMocks
import models.api.{VatEligibilityChoice, VatExpectedThresholdPostIncorp, VatScheme, VatThresholdPostIncorp}
import models.view.{ExpectationOverThresholdView, OverThresholdView, TaxableTurnover, VoluntaryRegistration, VoluntaryRegistrationReason}
import models.view.TaxableTurnover._
import models.view.VoluntaryRegistration._
import models.view.VoluntaryRegistrationReason._
import models.{CurrentProfile, S4LVatEligibility, S4LVatEligibilityChoice}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class EligibilityServiceSpec extends UnitSpec with MockitoSugar with VatMocks with FutureAssertions with VatRegistrationFixture with S4LFixture {
  implicit val hc = HeaderCarrier()

  class SetupWithCurrentProfileIncorpDate {
    implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1",
      VatRegStatus.draft,Some(LocalDate.of(2016, 12, 21)))

    val service = new EligibilityService {
      override val s4LConnector = mockS4LConnector
      override val vatRegistrationService = mockVatRegistrationService
    }

    when(mockS4LConnector.fetchAndGet(ArgumentMatchers.any(), ArgumentMatchers.any())( ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(None))
  }

  class SetupWithCurrentProfileNoIncorpDate {
    implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1",
      VatRegStatus.draft,None)

    val service = new EligibilityService {
      override val s4LConnector = mockS4LConnector
      override val vatRegistrationService = mockVatRegistrationService
    }

    when(mockS4LConnector.fetchAndGet(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(None))
  }

  class Setup2(newValue: S4LVatEligibility) {
    implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1",
      VatRegStatus.draft,None)

    val service = new EligibilityService {
      override val s4LConnector = mockS4LConnector
      override val vatRegistrationService = mockVatRegistrationService

      override def getEligibility(implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[S4LVatEligibility] =
        Future.successful(validS4LEligibility)

      override def saveEligibilityQuestions(newValue: S4LVatEligibility)
                                           (implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[S4LVatEligibility] =
        Future.successful(newValue)
    }
  }

  "Calling saveQuestion" should {
    Seq[(EligibilityQuestions.Value, S4LVatEligibility)](
      EligibilityQuestions.doingBusinessAbroad -> validS4LEligibility.copy(doingBusinessAbroad = Some(true)),
      EligibilityQuestions.doAnyApplyToYou -> validS4LEligibility.copy(doAnyApplyToYou = Some(true)),
      EligibilityQuestions.applyingForAnyOf -> validS4LEligibility.copy(applyingForAnyOf = Some(true)),
      EligibilityQuestions.applyingForVatExemption -> validS4LEligibility.copy(applyingForVatExemption = Some(true))
    ).foreach {
      case (key, expected) =>
        s"return an updated S4L Eligibility View model with new $key value" in new Setup2(expected) {
          service.saveQuestion(key, true) returns expected
        }
    }
  }

  "Calling getEligibilityChoice" should {
    "return a default empty S4L Eligibility Choice View Model when there is no data in S4L and backend" in new SetupWithCurrentProfileNoIncorpDate {
      val vatScheme = VatScheme(
        id = testRegId,
        status = VatRegStatus.draft,
        vatServiceEligibility = Some(validServiceEligibilityNoChoice)
      )

      when(mockVatRegistrationService.getVatScheme(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(vatScheme))

      when(mockS4LConnector.save(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(CacheMap("", Map())))

      service.getEligibilityChoice returns S4LVatEligibilityChoice()
    }

    "return a valid S4L View Model converted from backend data when necessity is voluntary and reason is SELLS" in new SetupWithCurrentProfileNoIncorpDate {
      val choice = VatEligibilityChoice(
        VatEligibilityChoice.NECESSITY_VOLUNTARY,
        Some(SELLS)
      )

      val vatScheme = VatScheme(
        id = testRegId,
        status = VatRegStatus.draft,
        vatServiceEligibility = Some(validServiceEligibility.copy(vatEligibilityChoice = Some(choice)))
      )

      when(mockVatRegistrationService.getVatScheme(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(vatScheme))

      when(mockS4LConnector.save(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("", Map())))

      val expected = S4LVatEligibilityChoice(
        Some(TaxableTurnover(TAXABLE_NO)),
        Some(VoluntaryRegistration(REGISTER_YES)),
        Some(VoluntaryRegistrationReason(SELLS))
      )

      service.getEligibilityChoice returns expected
    }

    "return a valid S4L View Model converted from backend data when necessity is voluntary and reason is INTENDS_TO_SELL" in
      new SetupWithCurrentProfileNoIncorpDate {

      val choice = VatEligibilityChoice(
        VatEligibilityChoice.NECESSITY_VOLUNTARY,
        Some(INTENDS_TO_SELL)
      )

      val vatScheme = VatScheme(
        id = testRegId,
        status = VatRegStatus.draft,
        vatServiceEligibility = Some(validServiceEligibility.copy(vatEligibilityChoice = Some(choice)))
      )

      when(mockVatRegistrationService.getVatScheme(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(vatScheme))

      when(mockS4LConnector.save(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("", Map())))

      val expected = S4LVatEligibilityChoice(
        Some(TaxableTurnover(TAXABLE_NO)),
        Some(VoluntaryRegistration(REGISTER_YES)),
        Some(VoluntaryRegistrationReason(INTENDS_TO_SELL))
      )

      service.getEligibilityChoice returns expected
    }

    "return a valid S4L View Model converted from backend data when necessity is voluntary and reason is NEITHER" in new SetupWithCurrentProfileNoIncorpDate {
      val choice = VatEligibilityChoice(
        VatEligibilityChoice.NECESSITY_VOLUNTARY,
        Some(NEITHER)
      )

      val vatScheme = VatScheme(
        id = testRegId,
        status = VatRegStatus.draft,
        vatServiceEligibility = Some(validServiceEligibility.copy(vatEligibilityChoice = Some(choice)))
      )

      when(mockVatRegistrationService.getVatScheme(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(vatScheme))

      when(mockS4LConnector.save(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("", Map())))

      val expected = S4LVatEligibilityChoice(
        Some(TaxableTurnover(TAXABLE_NO)),
        Some(VoluntaryRegistration(REGISTER_YES)),
        Some(VoluntaryRegistrationReason(NEITHER))
      )

      service.getEligibilityChoice returns expected
    }

    "return a valid S4L View Model converted from backend data when necessity is obligatory" in new SetupWithCurrentProfileNoIncorpDate {
      val choice = VatEligibilityChoice(
        VatEligibilityChoice.NECESSITY_OBLIGATORY,
        None
      )

      val vatScheme = VatScheme(
        id = testRegId,
        status = VatRegStatus.draft,
        vatServiceEligibility = Some(validServiceEligibility.copy(vatEligibilityChoice = Some(choice)))
      )

      when(mockVatRegistrationService.getVatScheme(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(vatScheme))

      when(mockS4LConnector.save(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("", Map())))

      val expected = S4LVatEligibilityChoice(
        Some(TaxableTurnover(TAXABLE_YES)),
        Some(VoluntaryRegistration(REGISTER_NO))
      )

      service.getEligibilityChoice returns expected
    }

    "return a valid S4L View Model converted from backend data when necessity is obligatory and over threshold are both true" in
      new SetupWithCurrentProfileIncorpDate {

      val choice = VatEligibilityChoice(
        VatEligibilityChoice.NECESSITY_OBLIGATORY,
        None,
        Some(VatThresholdPostIncorp(true, Some(LocalDate.of(2016, 12, 20)))),
        Some(VatExpectedThresholdPostIncorp(true, Some(LocalDate.of(2016, 12, 20))))
      )

      val vatScheme = VatScheme(
        id = testRegId,
        status = VatRegStatus.draft,
        vatServiceEligibility = Some(validServiceEligibility.copy(vatEligibilityChoice = Some(choice)))
      )

      when(mockVatRegistrationService.getVatScheme(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(vatScheme))

      when(mockS4LConnector.save(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(CacheMap("", Map())))

      val expected = S4LVatEligibilityChoice(
        None,
        Some(VoluntaryRegistration(REGISTER_NO)),
        None,
        Some(OverThresholdView(true, Some(LocalDate.of(2016, 12, 20)))),
        Some(ExpectationOverThresholdView(true, Some(LocalDate.of(2016, 12, 20))))
      )

      service.getEligibilityChoice returns expected
    }
  }
}
