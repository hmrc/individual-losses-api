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

package v6.bfLosses.delete.def1

import cats.data.Validated
import cats.implicits.catsSyntaxTuple3Semigroupal
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveNino, ResolveTaxYearMinMax}
import shared.models.domain.TaxYear
import shared.models.errors.{MtdError, RuleTaxYearForVersionNotSupportedError, RuleTaxYearNotSupportedError}
import v6.bfLosses.common.{maximumTaxYear, minimumTaxYear}
import v6.bfLosses.common.resolvers.ResolveBFLossId
import v6.bfLosses.delete.def1.model.request.Def1_DeleteBFLossRequestData
import v6.bfLosses.delete.model.request.DeleteBFLossRequestData

import javax.inject.Singleton

@Singleton
class Def1_DeleteBFLossValidator(nino: String, body: String, taxYear: String) extends Validator[DeleteBFLossRequestData] {

  private val minMaxTaxYears: (TaxYear, TaxYear) = (minimumTaxYear, maximumTaxYear)

  private val resolveTaxYear = ResolveTaxYearMinMax(
    minMaxTaxYears,
    minError = RuleTaxYearNotSupportedError,
    maxError = RuleTaxYearForVersionNotSupportedError
  )

  def validate: Validated[Seq[MtdError], DeleteBFLossRequestData] =
    (
      ResolveNino(nino),
      ResolveBFLossId(body),
      resolveTaxYear(taxYear)
    ).mapN(Def1_DeleteBFLossRequestData.apply)

}
