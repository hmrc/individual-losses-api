/*
 * Copyright 2022 HM Revenue & Customs
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

package api.validations.anyVersion

import api.models.domain.DownstreamTaxYear
import api.models.errors.{ MtdError, RuleTaxYearNotSupportedError }
import api.validations.NoValidationErrors

object MinTaxYearValidation {

  // @param taxYear In format YYYY-YY
  def validate(taxYear: String, minTaxYear: Int): Seq[MtdError] = {

    val downstreamTaxYear = Integer.parseInt(DownstreamTaxYear.fromMtd(taxYear).value)

    if (downstreamTaxYear >= minTaxYear) NoValidationErrors else List(RuleTaxYearNotSupportedError)
  }
}