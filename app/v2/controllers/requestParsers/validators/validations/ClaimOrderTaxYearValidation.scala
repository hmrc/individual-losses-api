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

package v2.controllers.requestParsers.validators.validations

import api.models.errors._
import v2.models.errors.RuleTaxYearNotSupportedError

object ClaimOrderTaxYearValidation {

  val taxYearFormat = "20[1-9][0-9]\\-[1-9][0-9]"

  def validate(taxYear: String): List[MtdError] = {
    if (taxYear.matches(taxYearFormat)) {

      val start     = taxYear.substring(2, 4).toInt
      val end       = taxYear.substring(5, 7).toInt
      val startYear = taxYear.substring(0, 4).toInt

      if (end - start == 1) {
        if (startYear < 2019) {
          List(RuleTaxYearNotSupportedError)
        } else {
          NoValidationErrors
        }
      } else {
        List(TaxYearFormatError)
      }
    } else {
      List(TaxYearFormatError)
    }
  }
}
