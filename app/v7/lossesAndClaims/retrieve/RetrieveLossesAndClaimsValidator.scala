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

package v7.lossesAndClaims.retrieve

import cats.data.Validated
import cats.data.Validated.*
import cats.implicits.*
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveBusinessId, ResolveNino, ResolveTaxYearMinimum}
import shared.models.domain.TaxYear
import shared.models.errors.MtdError
import v7.lossesAndClaims.minimumTaxYear
import v7.lossesAndClaims.retrieve.model.request.RetrieveLossesAndClaimsRequestData

class RetrieveLossesAndClaimsValidator(nino: String, businessId: String, taxYear: String) extends Validator[RetrieveLossesAndClaimsRequestData] {

  def resolvedTaxYear(taxYear: String): Validated[Seq[MtdError], TaxYear] = {
    ResolveTaxYearMinimum(
      minimumTaxYear
    )(taxYear)
  }

  def validate: Validated[Seq[MtdError], RetrieveLossesAndClaimsRequestData] = (
    ResolveNino(nino),
    ResolveBusinessId(businessId),
    resolvedTaxYear(taxYear)
  ).mapN(RetrieveLossesAndClaimsRequestData.apply)

}
