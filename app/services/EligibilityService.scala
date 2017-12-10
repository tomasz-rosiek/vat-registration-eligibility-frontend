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
import javax.inject.Inject

import common.enums.{CacheKeys, EligibilityQuestions => Questions}
import connectors.S4LConnector
import models.api.VatEligibilityChoice.{NECESSITY_OBLIGATORY, NECESSITY_VOLUNTARY}
import models.api._
import models.view.TaxableTurnover.{TAXABLE_NO, TAXABLE_YES}
import models.view.VoluntaryRegistration.{REGISTER_NO, REGISTER_YES}
import models.view.VoluntaryRegistrationReason.{INTENDS_TO_SELL, NEITHER, SELLS}
import models.view._
import models.{CurrentProfile, S4LVatEligibility, S4LVatEligibilityChoice}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class EligibilityServiceImpl @Inject()(val s4LConnector: S4LConnector,
                                       val vatRegistrationService: VatRegistrationService) extends EligibilityService

trait EligibilityService {
  val s4LConnector: S4LConnector
  val vatRegistrationService: VatRegistrationService

  //Eligibility Questions
  private def toEligibilityViewModel(scheme: VatScheme): S4LVatEligibility =
    scheme.vatServiceEligibility.map(e =>
      S4LVatEligibility(
        haveNino = e.haveNino,
        doingBusinessAbroad = e.doingBusinessAbroad,
        doAnyApplyToYou = e.doAnyApplyToYou,
        applyingForAnyOf = e.applyingForAnyOf,
        applyingForVatExemption = e.applyingForVatExemption,
        companyWillDoAnyOf = e.companyWillDoAnyOf
      )
    ).getOrElse(S4LVatEligibility())

  def toApi(viewModel: S4LVatEligibility): VatServiceEligibility =
    VatServiceEligibility(
      haveNino = viewModel.haveNino,
      doingBusinessAbroad = viewModel.doingBusinessAbroad,
      doAnyApplyToYou = viewModel.doAnyApplyToYou,
      applyingForAnyOf = viewModel.applyingForAnyOf,
      applyingForVatExemption = viewModel.applyingForVatExemption,
      companyWillDoAnyOf = viewModel.companyWillDoAnyOf
    )

  private def viewToApiModel(viewModel: S4LVatEligibility): Either[S4LVatEligibility, VatServiceEligibility] =
    viewModel match {
      case s4l@S4LVatEligibility(Some(_), Some(_), Some(_), Some(_), Some(_), Some(_)) => Right(toApi(s4l))
      case view => Left(view)
    }

  //Eligibility Choice
  private def toEligibilityChoiceViewModelWithoutIncorpDate(api: VatEligibilityChoice): S4LVatEligibilityChoice =
  {
    S4LVatEligibilityChoice(
      taxableTurnover = api.necessity match {
        case NECESSITY_VOLUNTARY  => Some(TaxableTurnover(TAXABLE_NO))
        case NECESSITY_OBLIGATORY => Some(TaxableTurnover(TAXABLE_YES))
        case _                    => None
      },
      voluntaryRegistration = api.necessity match {
        case NECESSITY_VOLUNTARY  => Some(VoluntaryRegistration(REGISTER_YES))
        case NECESSITY_OBLIGATORY => Some(VoluntaryRegistration(REGISTER_NO))
        case _                    => None
      },
      voluntaryRegistrationReason = api.reason.map {
        case SELLS            => VoluntaryRegistrationReason(SELLS)
        case INTENDS_TO_SELL  => VoluntaryRegistrationReason(INTENDS_TO_SELL)
        case NEITHER          => VoluntaryRegistrationReason(NEITHER)
      },
      overThreshold = None,
      expectationOverThreshold = None
    )
  }

  private def toEligibilityChoiceViewModelWithIncorpDate(api: VatEligibilityChoice): S4LVatEligibilityChoice =
  {
    S4LVatEligibilityChoice(
      taxableTurnover = None,
      voluntaryRegistration = api.necessity match {
        case NECESSITY_VOLUNTARY  => Some(VoluntaryRegistration(REGISTER_YES))
        case NECESSITY_OBLIGATORY => Some(VoluntaryRegistration(REGISTER_NO))
        case _                    => None
      },
      voluntaryRegistrationReason = api.reason.map {
        case SELLS           => VoluntaryRegistrationReason(SELLS)
        case INTENDS_TO_SELL => VoluntaryRegistrationReason(INTENDS_TO_SELL)
        case NEITHER         => VoluntaryRegistrationReason(NEITHER)
      },
      overThreshold = api.vatThresholdPostIncorp.map { t =>
        OverThresholdView(t.overThresholdSelection, t.overThresholdDate)
      },
      expectationOverThreshold = api.vatExpectedThresholdPostIncorp.map { exp =>
        ExpectationOverThresholdView(exp.expectedOverThresholdSelection, exp.expectedOverThresholdDate)
      }
    )
  }

  private def toEligibilityChoiceViewModel(api: VatScheme, incorpDate: Option[LocalDate]): S4LVatEligibilityChoice =
    api.vatServiceEligibility.map { e =>
      e.vatEligibilityChoice.map { ec =>
        if (incorpDate.isDefined) {
          toEligibilityChoiceViewModelWithIncorpDate(ec)
        } else {
          toEligibilityChoiceViewModelWithoutIncorpDate(ec)
        }
      }.getOrElse(S4LVatEligibilityChoice())
    }.getOrElse(S4LVatEligibilityChoice())

  def toApi(viewModel: S4LVatEligibilityChoice): VatEligibilityChoice =
    VatEligibilityChoice(
      necessity = viewModel.voluntaryRegistration.map { vr =>
        if (vr.yesNo == REGISTER_YES) NECESSITY_VOLUNTARY else NECESSITY_OBLIGATORY
      }.getOrElse(NECESSITY_OBLIGATORY),
      reason = viewModel.voluntaryRegistrationReason.map(_.reason),
      vatThresholdPostIncorp = viewModel.overThreshold.map(vtp => VatThresholdPostIncorp(vtp.selection, vtp.date)),
      vatExpectedThresholdPostIncorp = viewModel.expectationOverThreshold.map(eot => VatExpectedThresholdPostIncorp(eot.selection, eot.date))
    )

  private def viewToApiModel(viewModel: S4LVatEligibilityChoice): Either[S4LVatEligibilityChoice, VatEligibilityChoice] =
    viewModel match {
      case s4l@S4LVatEligibilityChoice(
      Some(TaxableTurnover(TAXABLE_NO)),
      Some(VoluntaryRegistration(REGISTER_YES)),
      Some(VoluntaryRegistrationReason(_)),
      None,
      None) => Right(toApi(s4l))
      case s4l@S4LVatEligibilityChoice(
      Some(TaxableTurnover(TAXABLE_YES)),
      Some(VoluntaryRegistration(REGISTER_NO)),
      None,
      None,
      None) => Right(toApi(s4l))
      case s4l@S4LVatEligibilityChoice(
      None,
      Some(VoluntaryRegistration(REGISTER_YES)),
      Some(VoluntaryRegistrationReason(_)),
      Some(OverThresholdView(false, _)),
      Some(ExpectationOverThresholdView(false, _))) => Right(toApi(s4l))
      case s4l@S4LVatEligibilityChoice(
      None,
      Some(VoluntaryRegistration(REGISTER_NO)),
      None,
      Some(OverThresholdView(_, _)),
      Some(ExpectationOverThresholdView(_, _))) => Right(toApi(s4l))
      case view => Left(view)
    }

  def getEligibility(implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[S4LVatEligibility] =
    s4LConnector.fetchAndGet[S4LVatEligibility](currentProfile.registrationId, CacheKeys.Eligibility) flatMap {
      case Some(s4lModel) => Future.successful(s4lModel)
      case None           => for {
        scheme <- vatRegistrationService.getVatScheme
        toSave = toEligibilityViewModel(scheme)
        _ <- s4LConnector.save(currentProfile.registrationId, CacheKeys.Eligibility, toSave)
      } yield toSave
    }

  def getEligibilityChoice(implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[S4LVatEligibilityChoice] =
    s4LConnector.fetchAndGet[S4LVatEligibilityChoice](currentProfile.registrationId, CacheKeys.EligibilityChoice) flatMap {
      case Some(s4lModel) => Future.successful(s4lModel)
      case None           => for {
        scheme <- vatRegistrationService.getVatScheme
        toSave = toEligibilityChoiceViewModel(scheme, currentProfile.incorporationDate)
        _      <- s4LConnector.save(currentProfile.registrationId, CacheKeys.EligibilityChoice, toSave)
      } yield toSave
    }

  def saveQuestion(questionKey: Questions.Value, newValue: Boolean)(implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[S4LVatEligibility] =
    for {
      eligibility <- getEligibility
      toSave = questionKey match {
        case Questions.haveNino                => eligibility.copy(haveNino = Some(newValue))
        case Questions.doingBusinessAbroad     => eligibility.copy(doingBusinessAbroad = Some(newValue))
        case Questions.doAnyApplyToYou         => eligibility.copy(doAnyApplyToYou = Some(newValue))
        case Questions.applyingForAnyOf        => eligibility.copy(applyingForAnyOf = Some(newValue))
        case Questions.applyingForVatExemption => eligibility.copy(applyingForVatExemption = Some(newValue))
        case Questions.companyWillDoAnyOf      => eligibility.copy(companyWillDoAnyOf = Some(newValue))
      }
      res <- saveEligibilityQuestions(toSave)
    } yield res

  private[services] def saveEligibilityQuestions(newValue: S4LVatEligibility)
                                                (implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[S4LVatEligibility] =
    viewToApiModel(newValue) match {
      case Right(api) => for {
        choice <- getEligibilityChoice
        toSave = viewToApiModel(choice) match {
          case Right(c) => api.copy(vatEligibilityChoice = Some(c))
          case Left(_) => api
        }
        _ <- saveEligibility(toSave)
      } yield newValue
      case Left(view) => s4LConnector.save(currentProfile.registrationId, CacheKeys.Eligibility, view) map (_ => newValue)
    }

  private def updateEligibilityChoiceViewModel(viewModel: S4LVatEligibilityChoice): S4LVatEligibilityChoice = viewModel match {
    case S4LVatEligibilityChoice(Some(TaxableTurnover(TAXABLE_YES)), _, _, _, _) =>
      viewModel.copy(voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_NO)), voluntaryRegistrationReason = None)
    case S4LVatEligibilityChoice(None, _, _, Some(OverThresholdView(s1, _)), Some(ExpectationOverThresholdView(s2, _))) if s1 || s2 =>
      viewModel.copy(voluntaryRegistration = Some(VoluntaryRegistration(REGISTER_NO)), voluntaryRegistrationReason = None)
    case S4LVatEligibilityChoice(_, Some(VoluntaryRegistration(REGISTER_NO)), Some(_), _, _) =>
      viewModel.copy(voluntaryRegistrationReason = None)
    case _ => viewModel
  }

  private def buildEligibilityChoiceViewModel(viewModel: S4LVatEligibilityChoice, newValue: EligibilityChoice): S4LVatEligibilityChoice = {
    val newViewModel = newValue match {
      case vr: VoluntaryRegistration          => viewModel.copy(voluntaryRegistration = Some(vr))
      case vrr: VoluntaryRegistrationReason   => viewModel.copy(voluntaryRegistrationReason = Some(vrr))
      case tt: TaxableTurnover                => viewModel.copy(taxableTurnover = Some(tt))
      case ot: OverThresholdView              => viewModel.copy(overThreshold = Some(ot))
      case eot: ExpectationOverThresholdView  => viewModel.copy(expectationOverThreshold = Some(eot))
    }

    updateEligibilityChoiceViewModel(newViewModel)
  }

  def saveChoiceQuestion(newValue: EligibilityChoice)(implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[S4LVatEligibilityChoice] =
    for {
      eligibilityChoice <- getEligibilityChoice
      toSave = buildEligibilityChoiceViewModel(eligibilityChoice, newValue)
      _ <- saveChoiceQuestions(toSave)
    } yield toSave

  private def saveChoiceQuestions(newValue: S4LVatEligibilityChoice)
                                 (implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[S4LVatEligibilityChoice] =
    viewToApiModel(newValue) match {
      case Right(api) => for {
        eligibility <- getEligibility
        toSave = viewToApiModel(eligibility) match {
          case Right(apiEligibility) => apiEligibility.copy(vatEligibilityChoice = Some(api))
          case Left(_) => throw new IllegalStateException("Eligibility questions values are missing")
        }
        _ <- saveEligibility(toSave)
      } yield newValue
      case Left(view) => s4LConnector.save(currentProfile.registrationId, CacheKeys.EligibilityChoice, view) map (_ => newValue)
    }

  private def saveEligibility(newValue: VatServiceEligibility)(implicit currentProfile: CurrentProfile, hc: HeaderCarrier): Future[VatServiceEligibility] = {
    for {
      _ <- vatRegistrationService.submitEligibility(newValue)
      _ <- s4LConnector.clear(currentProfile.registrationId)
    } yield newValue
  }
}
