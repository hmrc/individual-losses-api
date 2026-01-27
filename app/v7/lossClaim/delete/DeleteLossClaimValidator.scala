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

package v7.lossClaim.delete

import cats.data.Validated
import cats.implicits.catsSyntaxTuple3Semigroupal
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveBusinessId, ResolveNino, ResolveTaxYearMinimum}
import shared.models.errors.MtdError
import v7.bfLosses.common.minimumTaxYear26_27
import v7.lossClaim.delete.model.request.DeleteLossClaimRequestData

import javax.inject.Singleton

@Singleton
class DeleteLossClaimValidator(nino: String, businessId: String, taxYear: String) extends Validator[DeleteLossClaimRequestData] {

  private val resolveTaxYear: ResolveTaxYearMinimum = ResolveTaxYearMinimum(minimumTaxYear26_27)

  def validate: Validated[Seq[MtdError], DeleteLossClaimRequestData] =
    (
      ResolveNino(nino),
      ResolveBusinessId(businessId),
      resolveTaxYear(taxYear)
    ).mapN(DeleteLossClaimRequestData.apply)

}
