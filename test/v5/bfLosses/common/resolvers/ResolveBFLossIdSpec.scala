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

package v5.bfLosses.common.resolvers

import common.errors.LossIdFormatError
import cats.data.Validated.{Invalid, Valid}
import shared.utils.UnitSpec
import v5.bfLosses.common.domain.LossId

class ResolveBFLossIdSpec extends UnitSpec {

  private val validLossId   = "AAZZ1234567890a"
  private val invalidLossId = "AAZZ1234567890"

  "ResolveLossId" should {
    "return the resolved LossId" when {
      "given a valid LossId" in {
        val result = ResolveBFLossId(validLossId)
        result shouldBe Valid(LossId(validLossId))
      }
    }

    "return a LossIdFormatError" when {
      "given an invalid LossId" in {
        val result = ResolveBFLossId(invalidLossId)
        result shouldBe Invalid(List(LossIdFormatError))
      }
    }
  }

}
