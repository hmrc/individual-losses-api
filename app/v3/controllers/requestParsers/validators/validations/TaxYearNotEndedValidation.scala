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

package v3.controllers.requestParsers.validators.validations

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import utils.CurrentDate
import v3.models.domain.DownstreamTaxYear
import v3.models.errors.{MtdError, RuleTaxYearNotEndedError}

object TaxYearNotEndedValidation {

  // @param taxYear In format YYYY-YY
  def validate(taxYear: String)(implicit dateProvider: CurrentDate): List[MtdError] = {

    val downstreamTaxYear = Integer.parseInt(DownstreamTaxYear.fromMtd(taxYear).value)
    val currentDate: LocalDate = dateProvider.getCurrentDate

    if (downstreamTaxYear >= getCurrentTaxYear(currentDate)) List(RuleTaxYearNotEndedError) else NoValidationErrors
  }

  private def getCurrentTaxYear(date: LocalDate): Int = {

    lazy val taxYearStartDate: LocalDate = LocalDate.parse(
      date.getYear + "-04-06",
      DateTimeFormatter.ofPattern("yyyy-MM-dd")
    )

    if (date.isBefore(taxYearStartDate)) date.getYear else date.getYear + 1
  }
}
