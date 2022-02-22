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

package v3.models.domain

import support.UnitSpec

import java.time.LocalDate

class DownstreamTaxYearSpec extends UnitSpec {

  "DownstreamTaxYear" when {
    "constructed from an mtd tax year" should {
      "use the end year as the basis for the downstream tax year" in {
        DownstreamTaxYear.fromMtd("2018-19") shouldBe DownstreamTaxYear("2019")
      }
    }

    "converted to an mtd tax year" should {
      "construct a 1-year range from the previous year" in {
        DownstreamTaxYear("2019").toMtd shouldBe "2018-19"
      }
    }
    "generate the most recent tax year" when {
      "the date is before XXXX-04-05" in {
        DownstreamTaxYear.mostRecentTaxYear(LocalDate.parse("2020-04-01")) shouldBe DownstreamTaxYear("2019")
      }
      "the date is after XXXX-04-05" in {
        DownstreamTaxYear.mostRecentTaxYear(LocalDate.parse("2020-04-13")) shouldBe DownstreamTaxYear("2020")
      }
    }
  }

}
