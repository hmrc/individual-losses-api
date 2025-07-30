/*
 * Copyright 2023 HM Revenue & Customs
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

package v5.lossClaims.create.def1

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.*
import common.errors.{RuleTypeOfClaimInvalid, TaxYearClaimedForFormatError, TypeOfClaimFormatError, TypeOfLossFormatError}
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveBusinessId, ResolveJsonObject, ResolveNino, ResolveTaxYearMinimum}
import shared.models.errors.{MtdError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError}
import v5.lossClaims.common.minimumTaxYear
import v5.lossClaims.common.models.TypeOfClaim.*
import v5.lossClaims.common.models.TypeOfLoss.*
import v5.lossClaims.common.resolvers.{ResolveLossClaimTypeOfLossFromJson, ResolveLossTypeOfClaimFromJson}
import v5.lossClaims.create.def1.model.request.{Def1_CreateLossClaimRequestBody, Def1_CreateLossClaimRequestData}
import v5.lossClaims.create.model.request.CreateLossClaimRequestData

class Def1_CreateLossClaimValidator(nino: String, body: JsValue) extends Validator[CreateLossClaimRequestData] {

  def validate: Validated[Seq[MtdError], CreateLossClaimRequestData] = {
    val resolveJson = new ResolveJsonObject[Def1_CreateLossClaimRequestBody]()

    validateRequestBodyEnums
      .andThen(_ =>
        (
          ResolveNino(nino),
          resolveJson(body)
        ).mapN(Def1_CreateLossClaimRequestData)
          .andThen(validateParsedData))
  }

  private def validateRequestBodyEnums: Validated[Seq[MtdError], Unit] =
    combine(
      ResolveLossClaimTypeOfLossFromJson(body, Some(TypeOfLossFormatError.withPath("/typeOfLoss"))),
      ResolveLossTypeOfClaimFromJson(body, Some(TypeOfClaimFormatError.withPath("/typeOfClaim")))
    )

  private def validateParsedData(parsed: Def1_CreateLossClaimRequestData): Validated[Seq[MtdError], Def1_CreateLossClaimRequestData] = {
    val resolveTaxYear = ResolveTaxYearMinimum(
      minimumTaxYear,
      notSupportedError = RuleTaxYearNotSupportedError.withPath("/taxYearClaimedFor"),
      formatError = TaxYearClaimedForFormatError.withPath("/taxYearClaimedFor"),
      rangeError = RuleTaxYearRangeInvalidError.withPath("/taxYearClaimedFor")
    )

    combine(
      resolveTaxYear(parsed.lossClaim.taxYearClaimedFor),
      ResolveBusinessId(parsed.lossClaim.businessId),
      validateRule(parsed)
    ).map(_ => parsed)
  }

  private def validateRule(parsed: Def1_CreateLossClaimRequestData): Validated[Seq[MtdError], Unit] = {

    def valid   = Valid(())
    def invalid = Invalid(List(RuleTypeOfClaimInvalid))

    (parsed.lossClaim.typeOfLoss, parsed.lossClaim.typeOfClaim) match {
      case (`self-employment`, `carry-forward` | `carry-sideways`) => valid
      case (`self-employment`, _)                                  => invalid

      case (`uk-property` | `foreign-property`, `carry-sideways` | `carry-sideways-fhl` | `carry-forward-to-carry-sideways`) => valid
      case (`uk-property` | `foreign-property`, _)                                                                           => invalid
    }
  }

}
