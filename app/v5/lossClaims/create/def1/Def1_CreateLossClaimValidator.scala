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

import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{DetailedResolveTaxYear, ResolveBusinessId, ResolveJsonObject, ResolveNino}
import api.models.errors.{RuleTypeOfClaimInvalid, TaxYearClaimedForFormatError}
import shared.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import play.api.libs.json.JsValue
import v4.controllers.validators.minimumTaxYearLossClaim
import v4.controllers.validators.resolvers.{ResolveLossClaimTypeOfLossFromJson, ResolveLossTypeOfClaimFromJson}
import v4.models.domain.lossClaim.TypeOfClaim.{`carry-forward-to-carry-sideways`, `carry-forward`, `carry-sideways-fhl`, `carry-sideways`}
import v4.models.domain.lossClaim.TypeOfLoss.{`foreign-property`, `self-employment`, `uk-property-non-fhl`}
import v5.lossClaims.create.def1.model.request.{Def1_CreateLossClaimRequestBody, Def1_CreateLossClaimRequestData}
import v5.lossClaims.create.model.request.CreateLossClaimRequestData

class Def1_CreateLossClaimValidator(nino: String, body: JsValue) extends Validator[CreateLossClaimRequestData] {

  private val resolveJson = new ResolveJsonObject[Def1_CreateLossClaimRequestBody]()

  private val resolveTaxYear =
    DetailedResolveTaxYear(maybeMinimumTaxYear = Some(minimumTaxYearLossClaim))

  def validate: Validated[Seq[MtdError], CreateLossClaimRequestData] =
    validateRequestBodyEnums
      .andThen(_ =>
        (
          ResolveNino(nino),
          resolveJson(body)
        ).mapN(Def1_CreateLossClaimRequestData)
          .andThen(validateParsedData))

  private def validateRequestBodyEnums: Validated[Seq[MtdError], Unit] =
    combine(
      ResolveLossClaimTypeOfLossFromJson(body, None, errorPath = Some("/typeOfLoss")),
      ResolveLossTypeOfClaimFromJson(body, None, errorPath = Some("/typeOfClaim"))
    )

  private def validateParsedData(parsed: Def1_CreateLossClaimRequestData): Validated[Seq[MtdError], Def1_CreateLossClaimRequestData] = {
    combine(
      resolveTaxYear(parsed.lossClaim.taxYearClaimedFor, Some(TaxYearClaimedForFormatError), Some("/taxYearClaimedFor")),
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

      case (`uk-property-non-fhl` | `foreign-property`, `carry-sideways` | `carry-sideways-fhl` | `carry-forward-to-carry-sideways`) => valid
      case (`uk-property-non-fhl` | `foreign-property`, _)                                                                           => invalid
    }
  }

}
