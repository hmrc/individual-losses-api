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

package v7.lossesAndClaims.createAmend.request

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.{catsSyntaxTuple2Semigroupal, catsSyntaxTuple4Semigroupal, toFoldableOps}
import play.api.libs.json.*
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.*
import shared.models.domain.TaxYear
import shared.models.errors.*
import v7.lossesAndClaims.commons.PreferenceOrderEnum
import v7.lossesAndClaims.minimumTaxYear

import javax.inject.Singleton

@Singleton
class CreateAmendLossesAndClaimsValidator(nino: String, businessId: String, taxYear: String, body: JsValue, temporalValidationEnabled: Boolean)
    extends Validator[CreateAmendLossesAndClaimsRequestData] {

  private val resolveJson = new ResolveJsonObject[CreateAmendLossesAndClaimsRequestBody]()

  def resolvedTaxYear(taxYear: String): Validated[Seq[MtdError], TaxYear] = {

    ResolveTaxYearMinimum(
      minimumTaxYear,
      notSupportedError = RuleTaxYearNotSupportedError,
      rangeError = RuleTaxYearRangeInvalidError,
      allowIncompleteTaxYear = !temporalValidationEnabled,
      taxYearNotEnded = RuleTaxYearNotEndedError
    )(taxYear)
  }

  def validate: Validated[Seq[MtdError], CreateAmendLossesAndClaimsRequestData] = {
    validateRequestBody(body)
      .andThen(_ =>
        (
          ResolveNino(nino),
          ResolveBusinessId(businessId),
          resolvedTaxYear(taxYear),
          resolveJson(body)
        ).mapN(CreateAmendLossesAndClaimsRequestData.apply))
  }

  private val doubleValuedPath = Seq(
    __ \ "claims" \ "carryBack" \ "previousYearGeneralIncome",
    __ \ "claims" \ "carryBack" \ "earlyYearLosses",
    __ \ "claims" \ "carryBack" \ "terminalLosses",
    __ \ "claims" \ "carrySideways" \ "currentYearGeneralIncome",
    __ \ "claims" \ "carryForward" \ "currentYearLosses",
    __ \ "claims" \ "carryForward" \ "previousYearsLosses",
    __ \ "losses" \ "broughtForwardLosses"
  )

  private val preferenceOrderPath = __ \ "claims" \ "preferenceOrder"

  private val allPaths = doubleValuedPath :+ preferenceOrderPath

  private def validateRequestBody(json: JsValue): Validated[Seq[MtdError], Unit] = {
    implicit val js: JsValue = json
    val pathsDefined =
      allPaths.exists(path => path.asSingleJson(json).isDefined)

    if (!pathsDefined) {
      val undefinedPaths = allPaths.map(_.toString())
      Invalid(List(RuleIncorrectOrEmptyBodyError.withPaths(undefinedPaths)))
    } else {
      doubleValuedPath.traverse_(validateDoubleValuedPath).andThen { _ =>
        (json \ "claims").validate[Claims].asEither match {
          case Left(_) =>
            Validated.invalid(Seq(FormatPreferenceOrder.withPath(preferenceOrderPath.toString())))
          case Right(claims) =>
            validateClaims(claims)
        }
      }
    }
  }

  private def validateDoubleValuedPath(path: JsPath)(implicit json: JsValue): Validated[Seq[MtdError], Unit] = {
    path.readNullable[BigDecimal].reads(json) match {
      case JsSuccess(_, _) =>
        Valid(())
      case JsError(_) =>
        Invalid(List(ValueFormatError.withPath(path.toString)))
    }
  }

  private def validateClaims(claims: Claims): Validated[Seq[MtdError], Unit] = {
    val previousYearGeneralIncome =
      claims.carryBack.flatMap(_.previousYearGeneralIncome)

    val currentYearGeneralIncome =
      claims.carrySideways.flatMap(_.currentYearGeneralIncome)

    val applyFirst =
      claims.preferenceOrder.flatMap(_.applyFirst)
    (
      validateApplyFirstPresence(previousYearGeneralIncome, currentYearGeneralIncome, applyFirst),
      validateTerminalLossesVsCarryForward(claims)
    ).mapN((_, _) => ())
  }

  private def validateApplyFirstPresence(
      previousYearIncome: Option[BigDecimal],
      currentYearIncome: Option[BigDecimal],
      applyFirst: Option[PreferenceOrderEnum]
  ): Validated[Seq[MtdError], Unit] = {

    val path = preferenceOrderPath.toString()

    (previousYearIncome.isDefined, currentYearIncome.isDefined, applyFirst.isDefined) match
      case (true, true, false) =>
        Validated.invalid(Seq(RuleMissingPreferenceOrder.withPath(path)))

      case (true, false, true) | (false, true, true) | (false, false, true) =>
        Validated.invalid(Seq(RulePreferenceOrderNotAllowed.withPath(path)))

      case _ => Validated.valid(())
  }

  private def validateTerminalLossesVsCarryForward(
      claims: Claims
  ): Validated[Seq[MtdError], Unit] = {

    val terminalPath     = __ \ "claims" \ "carryBack" \ "terminalLosses"
    val carryForwardPath = __ \ "claims" \ "carryForward"

    val terminalLossesDefined = claims.carryBack.flatMap(_.terminalLosses).isDefined
    val carryForwardDefined   = claims.carryForward.isDefined

    if (terminalLossesDefined && carryForwardDefined)
      Validated.invalid(
        Seq(
          RuleCarryForwardAndTerminalLossNotAllowed
            .withPaths(List(terminalPath.toString, carryForwardPath.toString))
        ))
    else
      Validated.valid(())
  }

}
