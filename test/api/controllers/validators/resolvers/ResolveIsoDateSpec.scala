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

import api.models.errors.StartDateFormatError
import cats.data.Validated.{Invalid, Valid}
import support.UnitSpec

import java.time.LocalDate

class ResolveIsoDateSpec extends UnitSpec {

  "ResolveIsoDate" should {
    "return the parsed date" when {
      "given a valid ISO date string" in {
        val validDate = "2024-06-21"
        val result    = ResolveIsoDate(validDate, Some(StartDateFormatError), None)
        result shouldBe Valid(LocalDate.parse("2024-06-21"))
      }
    }

    "return an error" when {
      "given an invalid/non-ISO date string" in {
        val invalidDate = "not-a-date"
        val result      = ResolveIsoDate(invalidDate, Some(StartDateFormatError), None)
        result shouldBe Invalid(List(StartDateFormatError))
      }
    }
  }

}
