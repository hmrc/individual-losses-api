/*
 * Copyright 2027 HM Revenue & Customs
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

package v7.lossClaims.delete

import cats.data.Validated
import cats.implicits.catsSyntaxTuple3Semigroupal
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveBusinessId, ResolveNino, ResolveTaxYearMinimum}
import shared.models.domain.TaxYear
import shared.models.errors.{MtdError, RuleTaxYearRangeInvalidError}
import v7.bfLosses.common.minimumTaxYear26_27
import v7.lossClaims.delete.model.request.DeleteLossClaimsRequestData

import javax.inject.Singleton

@Singleton
class DeleteLossClaimsValidator(nino: String, businessId: String, taxYear: String) extends Validator[DeleteLossClaimsRequestData] {

  def resolvedTaxYear(taxYear: String, taxYearErrorPath: Option[String] = None): Validated[Seq[MtdError], TaxYear] = {
    def withPath(error: MtdError): MtdError = taxYearErrorPath.fold(error)(error.withPath)

    ResolveTaxYearMinimum(
      minimumTaxYear26_27,
      rangeError = withPath(RuleTaxYearRangeInvalidError)
    )(taxYear)
  }

  def validate: Validated[Seq[MtdError], DeleteLossClaimsRequestData] =
    (
      ResolveNino(nino),
      ResolveBusinessId(businessId),
      resolvedTaxYear(taxYear)
    ).mapN(DeleteLossClaimsRequestData.apply)

}
