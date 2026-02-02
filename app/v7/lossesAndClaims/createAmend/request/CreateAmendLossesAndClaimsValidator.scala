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
import cats.implicits.{catsSyntaxTuple4Semigroupal, toFoldableOps}
import play.api.libs.json.*
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.*
import shared.models.domain.TaxYear
import shared.models.errors.*
import v7.lossesAndClaims.minimumTaxYear

import javax.inject.Singleton

@Singleton
class CreateAmendLossesAndClaimsValidator(nino: String, businessId: String, taxYear: String, body: JsValue)
    extends Validator[CreateAmendLossesAndClaimsRequestData] {

  private val resolveJson = new ResolveJsonObject[CreateAmendLossesAndClaimsRequestBody]()

  def resolvedTaxYear(taxYear: String): Validated[Seq[MtdError], TaxYear] = {

    ResolveTaxYearMinimum(
      minimumTaxYear,
      notSupportedError = RuleTaxYearNotSupportedError,
      rangeError = RuleTaxYearRangeInvalidError,
      taxYearNotEnded = RuleTaxYearNotEndedError
    )(taxYear)
  }

  def validate: Validated[Seq[MtdError], CreateAmendLossesAndClaimsRequestData] = {
    formatValidate(body)
      .andThen(_ =>
        (
          ResolveNino(nino),
          ResolveBusinessId(businessId),
          resolvedTaxYear(taxYear),
          resolveJson(body)
        ).mapN(CreateAmendLossesAndClaimsRequestData.apply))
  }

  private val doublePaths = Seq(
    __ \ "claims" \ "carryBack" \ "previousYearGeneralIncome",
    __ \ "claims" \ "carryBack" \ "earlyYearLosses",
    __ \ "claims" \ "carryBack" \ "terminalLosses",
    __ \ "claims" \ "carrySideways" \ "currentYearGeneralIncome",
    __ \ "claims" \ "carryForward" \ "currentYearLosses",
    __ \ "claims" \ "carryForward" \ "previousYearsLosses",
    __ \ "losses" \ "broughtForwardLosses"
  )

  private def formatValidate(json: JsValue): Validated[Seq[MtdError], Unit] = {
    implicit val js: JsValue = json
    doublePaths.traverse_(optionalDouble)
  }

  private def optionalDouble(path: JsPath)(implicit json: JsValue): Validated[Seq[MtdError], Unit] =
    path.readNullable[BigDecimal].reads(json) match {
      case JsSuccess(_, _) =>
        Valid(())
      case JsError(_) =>
        Invalid(List(ValueFormatError.withPath(path.toString)))
    }

}
