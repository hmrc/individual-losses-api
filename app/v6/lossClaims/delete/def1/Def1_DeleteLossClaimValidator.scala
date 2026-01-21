/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.delete.def1

import cats.data.Validated
import cats.implicits.catsSyntaxTuple3Semigroupal
import common.errors.TaxYearClaimedForFormatError
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveNino, ResolveTaxYearMinMax}
import shared.models.errors.{MtdError, RuleTaxYearForVersionNotSupportedError, RuleTaxYearNotSupportedError}
import v6.lossClaims.common.{maximumTaxYear, minimumTaxYear}
import v6.lossClaims.common.resolvers.ResolveLossClaimId
import v6.lossClaims.delete.def1.model.request.Def1_DeleteLossClaimRequestData
import v6.lossClaims.delete.model.request.DeleteLossClaimRequestData

class Def1_DeleteLossClaimValidator(nino: String, claimId: String, taxYearClaimedFor: String) extends Validator[DeleteLossClaimRequestData] {

  private val resolveTaxYearClaimedFor = ResolveTaxYearMinMax(
    minMax = (minimumTaxYear, maximumTaxYear),
    minError = RuleTaxYearNotSupportedError,
    maxError = RuleTaxYearForVersionNotSupportedError,
    formatError = TaxYearClaimedForFormatError
  )

  def validate: Validated[Seq[MtdError], DeleteLossClaimRequestData] =
    (
      ResolveNino(nino),
      ResolveLossClaimId(claimId),
      resolveTaxYearClaimedFor(taxYearClaimedFor)
    ).mapN(Def1_DeleteLossClaimRequestData.apply)

}
