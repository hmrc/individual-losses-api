/*
 * Copyright 2023 HM Revenue & Customs
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

package api.models.domain

import support.UnitSpec

import java.time.LocalDate

class TaxYearRangeSpec extends UnitSpec {

  "fromMtd()" should {

    "return a TaxYearRange" when {
      "passed an MTD-format taxYear" in {
        val taxYear: TaxYear = TaxYear.fromMtd("2019-20")
        TaxYearRange.fromMtd("2019-20") shouldBe TaxYearRange(taxYear)
      }
    }
  }

  "from and to" should {
    "return the correct taxYearStart and taxYearEnd respectively" when {
      "a valid taxYear is entered" in {
        val range: TaxYearRange = TaxYearRange.fromMtd("2019-20")
        range.from.taxYearStart shouldBe "2019-04-06"
        range.to.taxYearEnd shouldBe "2020-04-05"
      }
    }
  }

  "todayMinus(years)" should {
    "return a TaxYearRange from the 'subtracted' tax year to the current tax year" in {
      val today          = LocalDate.parse("2023-04-01")
      val currentTaxYear = "2022-23"
      val expectedFrom   = TaxYear.fromMtd("2018-19")

      implicit val todaySupplier: () => LocalDate = () => today

      val result: TaxYearRange = TaxYearRange.todayMinus(years = 4)
      result shouldBe TaxYearRange(expectedFrom, TaxYear.fromMtd(currentTaxYear))
    }
  }

}
