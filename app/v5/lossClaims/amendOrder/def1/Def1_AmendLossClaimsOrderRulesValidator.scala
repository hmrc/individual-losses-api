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

package v5.lossClaims.amendOrder.def1

import api.models.errors._
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import shared.controllers.validators.RulesValidator
import shared.controllers.validators.resolvers.ResolveParsedNumber
import shared.models.errors.{MtdError, RuleTaxYearNotSupportedError}
import v4.controllers.validators.resolvers.ResolveLossClaimId
import v5.lossClaims.amendOrder.def1.model.request.Def1_AmendLossClaimsOrderRequestData

object Def1_AmendLossClaimsOrderRulesValidator extends RulesValidator[Def1_AmendLossClaimsOrderRequestData] {

  def validateBusinessRules(parsed: Def1_AmendLossClaimsOrderRequestData): Validated[Seq[MtdError], Def1_AmendLossClaimsOrderRequestData] =
    combine(
      validateTaxYear(parsed),
      validateListOfLossClaims(parsed),
      validateSequenceNumbers(parsed)
    ).map(_ => parsed)

  private def validateTaxYear(parsed: Def1_AmendLossClaimsOrderRequestData): Validated[Seq[MtdError], Unit] =
    if (parsed.taxYearClaimedFor.year < 2020)
      Invalid(List(RuleTaxYearNotSupportedError))
    else
      Valid(())

  private def validateSequenceNumbers(parsed: Def1_AmendLossClaimsOrderRequestData): Validated[Seq[MtdError], Unit] = {
    val sequenceNums = parsed.body.listOfLossClaims.map(_.sequence).sorted

    def startsWithOne =
      if (sequenceNums.headOption.contains(1)) Valid(()) else Invalid(List(RuleInvalidSequenceStart))

    def isContinuous = {
      val noGaps = sequenceNums.sliding(2).forall {
        case a :: b :: Nil => b - a == 1
        case _             => true
      }

      if (noGaps) Valid(()) else Invalid(List(RuleSequenceOrderBroken))
    }

    combine(startsWithOne, isContinuous)
  }

  private def validateListOfLossClaims(parsed: Def1_AmendLossClaimsOrderRequestData): Validated[Seq[MtdError], Unit] = {
    val results = parsed.body.listOfLossClaims.zipWithIndex.map { case (lossClaim, index) =>
      combine(
        ResolveLossClaimId(lossClaim.claimId, path = s"/listOfLossClaims/$index/claimId"),
        ResolveParsedNumber(min = 1, max = 99)(lossClaim.sequence, path = s"/listOfLossClaims/$index/sequence")
      )
    }

    combine(results: _*)
  }

}
