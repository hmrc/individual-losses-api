/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.createAmend

import cats.data.Validated
import cats.data.Validated.Invalid
import cats.implicits.*
import shared.controllers.validators.RulesValidator
import shared.controllers.validators.resolvers.*
import shared.models.errors.*
import v7.lossesAndClaims.commons.{Losses, PreferenceOrder, PreferenceOrderEnum}
import v7.lossesAndClaims.createAmend.request.*

object CreateAmendLossesAndClaimsRulesValidator extends RulesValidator[CreateAmendLossesAndClaimsRequestData] {

  private val resolveParsedNumber = ResolveParsedNumber()

  def validateBusinessRules(parsed: CreateAmendLossesAndClaimsRequestData): Validated[Seq[MtdError], CreateAmendLossesAndClaimsRequestData] = {
    import parsed.createAmendLossesAndClaimsRequestBody.*

    combine(
      validateClaims(claims),
      validateLosses(losses)
    ).onSuccess(parsed)
  }

  private def validateClaims(claims: Option[Claims]): Validated[Seq[MtdError], Unit] =
    claims.fold(valid) { claims =>
      combine(
        validateCarryBack(claims.carryBack),
        validateCarrySideways(claims.carrySideways),
        validateCarryForward(claims.carryForward),
        validatePreferenceOrderFormat(claims.preferenceOrder),
        validatePreferenceOrderRules(claims),
        validateTerminalLossesAndCarryForward(claims)
      )
    }

  private def validateCarryBack(carryBack: Option[CarryBack]): Validated[Seq[MtdError], Unit] =
    carryBack.fold(valid) { carryBack =>
      List(
        (carryBack.previousYearGeneralIncome, "/claims/carryBack/previousYearGeneralIncome"),
        (carryBack.earlyYearLosses, "/claims/carryBack/earlyYearLosses"),
        (carryBack.terminalLosses, "/claims/carryBack/terminalLosses")
      ).traverse_ { case (value, path) =>
        resolveParsedNumber(value, path)
      }
    }

  private def validateCarrySideways(carrySideways: Option[CarrySideways]): Validated[Seq[MtdError], Unit] =
    carrySideways.fold(valid) { carrySideways =>
      resolveParsedNumber(
        carrySideways.currentYearGeneralIncome,
        "/claims/carrySideways/currentYearGeneralIncome"
      ).toUnit
    }

  private def validatePreferenceOrderFormat(preferenceOrder: Option[PreferenceOrder]): Validated[Seq[MtdError], Unit] =
    preferenceOrder.fold(valid) { preferenceOrder =>
      preferenceOrder.applyFirst.fold(valid) { applyFirst =>
        resolveEnum(
          PreferenceOrderEnum.parser,
          FormatPreferenceOrder.withPath("/claims/preferenceOrder/applyFirst")
        )(applyFirst).toUnit
      }
    }

  private def validatePreferenceOrderRules(claims: Claims): Validated[Seq[MtdError], Unit] = {
    val preferenceOrderRequired: Boolean =
      claims.carryBack.exists(_.previousYearGeneralIncome.isDefined) &&
        claims.carrySideways.exists(_.currentYearGeneralIncome.isDefined)

    val preferenceOrderProvided: Boolean = claims.preferenceOrder.exists(_.applyFirst.isDefined)

    if (preferenceOrderRequired && !preferenceOrderProvided) {
      Invalid(List(RuleMissingPreferenceOrder.withPath("/claims")))
    } else if (!preferenceOrderRequired && preferenceOrderProvided) {
      Invalid(List(RulePreferenceOrderNotAllowed.withPath("/claims/preferenceOrder/applyFirst")))
    } else {
      valid
    }
  }

  private def validateCarryForward(carryForward: Option[CarryForward]): Validated[Seq[MtdError], Unit] =
    carryForward.fold(valid) { carryForward =>
      List(
        (carryForward.currentYearLosses, "/claims/carryForward/currentYearLosses"),
        (carryForward.previousYearsLosses, "/claims/carryForward/previousYearsLosses")
      ).traverse_ { case (value, path) =>
        resolveParsedNumber(value, path)
      }
    }

  private def validateLosses(losses: Option[Losses]): Validated[Seq[MtdError], Unit] =
    losses.fold(valid) { losses =>
      resolveParsedNumber(
        losses.broughtForwardLosses,
        "/losses/broughtForwardLosses"
      ).toUnit
    }

  private def validateTerminalLossesAndCarryForward(claims: Claims): Validated[Seq[MtdError], Unit] = {
    val terminalLossesDefined: Boolean = claims.carryBack.flatMap(_.terminalLosses).isDefined

    val carryForwardDefined: Boolean = claims.carryForward.isDefined

    if (terminalLossesDefined && carryForwardDefined) {
      Invalid(List(RuleCarryForwardAndTerminalLossNotAllowed.withPath("/claims")))
    } else {
      valid
    }
  }

}
