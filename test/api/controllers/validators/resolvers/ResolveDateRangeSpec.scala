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

package api.controllers.validators.resolvers

import api.models.domain.DateRange
import api.models.errors.{EndDateFormatError, RuleEndBeforeStartDateError, StartDateFormatError}
import cats.data.Validated.{Invalid, Valid}
import support.UnitSpec

import java.time.LocalDate

class ResolveDateRangeSpec extends UnitSpec {

  private val validStart = "2023-06-21"
  private val validEnd   = "2024-06-21"

  private val minYear = 1900
  private val maxYear = 2100

  private val dateResolver = ResolveDateRange.withLimits(minYear, maxYear)

  "ResolveDateRange" should {
    "return no errors" when {
      "given a valid start and end date" in {
        val result = dateResolver(validStart -> validEnd)
        result shouldBe Valid(DateRange(LocalDate.parse(validStart), LocalDate.parse(validEnd)))
      }

      "when both dates are at the bounds" in {
        val result = dateResolver("1900-01-01" -> "2099-12-31")
        result shouldBe Valid(DateRange(LocalDate.parse("1900-01-01"), LocalDate.parse("2099-12-31")))
      }

      "when date bounding is not in use" in {
        val unboundedResolver = ResolveDateRange.unlimited
        val result            = unboundedResolver("1567-01-01" -> "1678-01-31")

        result shouldBe Valid(DateRange(LocalDate.parse("1567-01-01"), LocalDate.parse("1678-01-31")))
      }
    }

    "return an error" when {
      "given an invalid start date" in {
        val result = dateResolver("not-a-date" -> validEnd)
        result shouldBe Invalid(List(StartDateFormatError))
      }

      "given an invalid end date" in {
        val result = dateResolver(validStart -> "not-a-date")
        result shouldBe Invalid(List(EndDateFormatError))
      }

      "given an end date before start date" in {
        val result = dateResolver(validEnd -> validStart)
        result shouldBe Invalid(List(RuleEndBeforeStartDateError))
      }

      "given a fromYear less than minimumYear" in {
        val result = dateResolver("1899-04-06" -> "2019-04-05")
        result shouldBe Invalid(List(StartDateFormatError))
      }

      "given a toYear greater than or equal to maximumYear" in {
        val result = dateResolver("2020-04-06" -> "2100-04-05")
        result shouldBe Invalid(List(EndDateFormatError))
      }

      "given both dates that are out of range" in {
        val result = dateResolver("0092-04-06" -> "2101-04-05")
        result shouldBe Invalid(List(StartDateFormatError, EndDateFormatError))
      }
    }
  }

}
