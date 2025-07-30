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

package v4.controllers.validators

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.*
import common.errors.{RuleTypeOfClaimInvalid, TaxYearClaimedForFormatError, TypeOfClaimFormatError, TypeOfLossFormatError}
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveBusinessId, ResolveJsonObject, ResolveNino, ResolveTaxYearMinimum}
import shared.models.errors.{MtdError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError}
import v4.controllers.validators.resolvers.{ResolveLossClaimTypeOfLossFromJson, ResolveLossTypeOfClaimFromJson}
import v4.models.domain.lossClaim.TypeOfClaim.{`carry-forward-to-carry-sideways`, `carry-forward`, `carry-sideways-fhl`, `carry-sideways`}
import v4.models.domain.lossClaim.TypeOfLoss.{`foreign-property`, `self-employment`, `uk-property-non-fhl`}
import v4.models.request.createLossClaim.{CreateLossClaimRequestBody, CreateLossClaimRequestData}

import javax.inject.Singleton

@Singleton
class CreateLossClaimValidatorFactory {

  private val resolveJson = new ResolveJsonObject[CreateLossClaimRequestBody]()

  def validator(nino: String, body: JsValue): Validator[CreateLossClaimRequestData] =
    new Validator[CreateLossClaimRequestData] {

      def validate: Validated[Seq[MtdError], CreateLossClaimRequestData] =
        validateRequestBodyEnums
          .andThen(_ =>
            (
              ResolveNino(nino),
              resolveJson(body)
            ).mapN(CreateLossClaimRequestData)
              .andThen(validateParsedData))

      private def validateRequestBodyEnums: Validated[Seq[MtdError], Unit] =
        combine(
          ResolveLossClaimTypeOfLossFromJson(body, Some(TypeOfLossFormatError.withPath("/typeOfLoss"))),
          ResolveLossTypeOfClaimFromJson(body, Some(TypeOfClaimFormatError.withPath("/typeOfClaim")))
        )

      private def validateParsedData(parsed: CreateLossClaimRequestData): Validated[Seq[MtdError], CreateLossClaimRequestData] = {
        val resolveTaxYear = ResolveTaxYearMinimum(
          minimumTaxYearLossClaim,
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

      private def validateRule(parsed: CreateLossClaimRequestData): Validated[Seq[MtdError], Unit] = {
        import parsed.lossClaim.{typeOfClaim, typeOfLoss}

        def valid   = Valid(())
        def invalid = Invalid(List(RuleTypeOfClaimInvalid))

        (typeOfLoss, typeOfClaim) match {
          case (`self-employment`, `carry-forward` | `carry-sideways`) => valid
          case (`self-employment`, _)                                  => invalid

          case (`uk-property-non-fhl` | `foreign-property`, `carry-sideways` | `carry-sideways-fhl` | `carry-forward-to-carry-sideways`) => valid
          case (`uk-property-non-fhl` | `foreign-property`, _)                                                                           => invalid
        }
      }

    }

}
