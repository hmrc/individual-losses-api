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

package api.validations.v3

import api.models.domain.TaxYear
import api.models.errors.{MtdError, RuleTaxYearNotEndedError}
import api.validations.NoValidationErrors
import utils.CurrentDate

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object TaxYearNotEndedValidation {

  // @param taxYear In format YYYY-YY
  def validate(taxYear: String)(implicit dateProvider: CurrentDate): Seq[MtdError] = {
    val downstreamTaxYear      = Integer.parseInt(TaxYear.fromMtd(taxYear).asDownstream)
    val currentDate: LocalDate = dateProvider.getCurrentDate

    if (downstreamTaxYear >= getCurrentTaxYear(currentDate)) List(RuleTaxYearNotEndedError) else NoValidationErrors
  }

  private def getCurrentTaxYear(date: LocalDate): Int = {
    lazy val taxYearStartDate: LocalDate = LocalDate.parse(
      s"$date.getYear,  -04-06",
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    if (date.isBefore(taxYearStartDate)) date.getYear else date.getYear + 1
  }
}
