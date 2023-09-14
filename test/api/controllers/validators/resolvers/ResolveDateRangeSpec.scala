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

import api.models.errors.{ EndDateFormatError, RuleEndBeforeStartDateError, StartDateFormatError }
import cats.data.Validated.{ Invalid, Valid }
import support.UnitSpec

import java.time.LocalDate

class ResolveDateRangeSpec extends UnitSpec {

  private val validStart = "2023-06-21"
  private val validEnd   = "2024-06-21"

  "ResolveDateRange" should {
    "return no errors" when {
      "passed a valid start and end date" in {
        val result = ResolveDateRange(validStart -> validEnd)
        result shouldBe Valid(DateRange(LocalDate.parse(validStart), LocalDate.parse(validEnd)))
      }
    }

    "return an error" when {
      "passed an invalid start date" in {
        val result = ResolveDateRange("not-a-date" -> validEnd)
        result shouldBe Invalid(List(StartDateFormatError))
      }

      "passed an invalid end date" in {
        val result = ResolveDateRange(validStart -> "not-a-date")
        result shouldBe Invalid(List(EndDateFormatError))
      }

      "passed an end date before start date" in {
        val result = ResolveDateRange(validEnd -> validStart)
        result shouldBe Invalid(List(RuleEndBeforeStartDateError))
      }
    }
  }
}
