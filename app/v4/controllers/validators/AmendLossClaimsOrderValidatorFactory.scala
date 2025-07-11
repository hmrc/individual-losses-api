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
import cats.implicits._
import common.errors.{ClaimIdFormatError, RuleInvalidSequenceStart, RuleSequenceOrderBroken, TaxYearClaimedForFormatError, TypeOfClaimFormatError}
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.ResolveTaxYear.resolverWithCustomErrors
import shared.controllers.validators.resolvers.{ResolveJsonObject, ResolveNino, ResolveParsedNumber}
import shared.models.errors._
import v4.controllers.validators.resolvers.{ResolveLossClaimId, ResolveLossTypeOfClaimFromJson}
import v4.models.domain.lossClaim.TypeOfClaim
import v4.models.request.amendLossClaimsOrder.{AmendLossClaimsOrderRequestBody, AmendLossClaimsOrderRequestData}

import javax.inject.Singleton
import scala.math.Ordering.Implicits.infixOrderingOps

@Singleton
class AmendLossClaimsOrderValidatorFactory {

  private val resolveJson    = new ResolveJsonObject[AmendLossClaimsOrderRequestBody]()
  private val resolveTaxYear = resolverWithCustomErrors(TaxYearClaimedForFormatError, RuleTaxYearRangeInvalidError)

  def validator(nino: String, taxYearClaimedFor: String, body: JsValue): Validator[AmendLossClaimsOrderRequestData] =
    new Validator[AmendLossClaimsOrderRequestData] {

      def validate: Validated[Seq[MtdError], AmendLossClaimsOrderRequestData] =
        ResolveLossTypeOfClaimFromJson(body)
          .andThen(validatePermittedTypeOfClaim)
          .andThen(_ =>
            (
              ResolveNino(nino),
              resolveTaxYear(taxYearClaimedFor),
              resolveJson(body)
            ).mapN(AmendLossClaimsOrderRequestData) andThen validateBusinessRules)

      private def validatePermittedTypeOfClaim(maybeTypeOfClaim: Option[TypeOfClaim]): Validated[Seq[MtdError], Unit] = {
        maybeTypeOfClaim match {
          case Some(typeOfClaim) if typeOfClaim == TypeOfClaim.`carry-sideways` =>
            Valid(())
          case Some(_) => Invalid(List(TypeOfClaimFormatError))
          case None    => Valid(())
        }
      }

      private def validateBusinessRules(parsed: AmendLossClaimsOrderRequestData): Validated[Seq[MtdError], AmendLossClaimsOrderRequestData] =
        combine(
          validateTaxYear(parsed),
          validateListOfLossClaims(parsed),
          validateSequenceNumbers(parsed)
        ).map(_ => parsed)

      private def validateTaxYear(parsed: AmendLossClaimsOrderRequestData): Validated[Seq[MtdError], Unit] =
        if (parsed.taxYearClaimedFor < minimumTaxYearLossClaim)
          Invalid(List(RuleTaxYearNotSupportedError))
        else
          Valid(())

      private def validateSequenceNumbers(parsed: AmendLossClaimsOrderRequestData): Validated[Seq[MtdError], Unit] = {
        val sequenceNums = parsed.body.listOfLossClaims.map(_.sequence).sorted

        def startsWithOne: Validated[Seq[MtdError], Unit] =
          if (sequenceNums.headOption.contains(1)) Valid(()) else Invalid(List(RuleInvalidSequenceStart))

        def isContinuous: Validated[Seq[MtdError], Unit] = {
          val noGaps = sequenceNums.sliding(2).forall {
            case a :: b :: Nil => b - a == 1
            case _             => true
          }

          if (noGaps) Valid(()) else Invalid(List(RuleSequenceOrderBroken))
        }

        combine(startsWithOne, isContinuous)
      }

      private def validateListOfLossClaims(parsed: AmendLossClaimsOrderRequestData): Validated[Seq[MtdError], Unit] = {
        val results = parsed.body.listOfLossClaims.zipWithIndex.map { case (lossClaim, index) =>
          combine(
            ResolveLossClaimId(lossClaim.claimId, Some(ClaimIdFormatError.withPath(s"/listOfLossClaims/$index/claimId"))),
            ResolveParsedNumber(min = 1, max = 99)(lossClaim.sequence, path = s"/listOfLossClaims/$index/sequence")
          )
        }

        combine(results: _*)
      }

    }

}
