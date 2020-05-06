/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.models.requestData

import java.time.LocalDate

import support.UnitSpec

class DesTaxYearSpec extends UnitSpec {

  val taxYear    = "2018-19"
  val desTaxYear = "2019"

  "DesTaxYear" should {
    "generate a des tax year" when {
      "given a year" in {
        val year = DesTaxYear.fromMtd(taxYear)
        year.value shouldBe desTaxYear
      }
    }
    "generate an mtd tax year" when {
      "given a year" in {
        val year = DesTaxYear.fromDes(desTaxYear)
        year.value shouldBe taxYear
      }
    }
    "generate the most recent tax year" when {
      "the date is before XXXX-04-05" in {
        DesTaxYear.mostRecentTaxYear(LocalDate.parse("2020-04-01")) shouldBe DesTaxYear("2019")
      }
      "the date is after XXXX-04-05" in {
        DesTaxYear.mostRecentTaxYear(LocalDate.parse("2020-04-13")) shouldBe DesTaxYear("2020")
      }
    }
  }

}
