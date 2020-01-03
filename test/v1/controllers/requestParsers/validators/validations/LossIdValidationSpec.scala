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

package v1.controllers.requestParsers.validators.validations

import support.UnitSpec
import v1.models.errors.LossIdFormatError
import v1.models.utils.JsonErrorValidators

class LossIdValidationSpec extends UnitSpec with JsonErrorValidators {

  private val validLossId   = "AAZZ1234567890a"
  private val invalidLossId = "AAZZ1234567890"

  "LossIdValidation.validate" should {
    "return an empty list" when {
      "passed a valid lossId" in {
        LossIdValidation.validate(validLossId).length shouldBe 0
      }
    }
    "return a non-empty list" when {
      "passed an invalid lossId" in {
        val result = LossIdValidation.validate(invalidLossId)
        result.length shouldBe 1
        result.head shouldBe LossIdFormatError
      }
    }
  }

}
