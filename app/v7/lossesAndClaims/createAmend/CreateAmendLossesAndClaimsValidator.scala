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

package v7.lossesAndClaims.createAmend

import cats.data.Validated
import cats.implicits.catsSyntaxTuple4Semigroupal
import play.api.libs.json.*
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.*
import shared.models.domain.TaxYear
import shared.models.errors.*
import v7.lossesAndClaims.createAmend.CreateAmendLossesAndClaimsRulesValidator.validateBusinessRules
import v7.lossesAndClaims.createAmend.request.{CreateAmendLossesAndClaimsRequestBody, CreateAmendLossesAndClaimsRequestData}

class CreateAmendLossesAndClaimsValidator(nino: String, businessId: String, taxYear: String, body: JsValue, temporalValidationEnabled: Boolean)
    extends Validator[CreateAmendLossesAndClaimsRequestData] {

  private val resolveJson = new ResolveNonEmptyJsonObject[CreateAmendLossesAndClaimsRequestBody]()

  private val resolveTaxYear = ResolveTaxYearMinimum(
    minimumTaxYear = TaxYear.fromMtd("2026-27"),
    allowIncompleteTaxYear = !temporalValidationEnabled
  )

  def validate: Validated[Seq[MtdError], CreateAmendLossesAndClaimsRequestData] = {
    (
      ResolveNino(nino),
      ResolveBusinessId(businessId),
      resolveTaxYear(taxYear),
      resolveJson(body)
    ).mapN(CreateAmendLossesAndClaimsRequestData.apply) andThen validateBusinessRules
  }

}
