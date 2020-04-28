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
import v1.models.errors.SequenceFormatError

class SequenceValidationSpec extends UnitSpec {

  "validate" should {
    "return no errors" when {
      "The minimum number is supplied" in {
        val sequenceNumber = 1
        val validationResult = SequenceValidation.validate(sequenceNumber)
        validationResult.isEmpty shouldBe true
      }
      "The maximum number is supplied" in {
        val sequenceNumber = 99
        val validationResult = SequenceValidation.validate(sequenceNumber)
        validationResult.isEmpty shouldBe true
      }
    }
    "return an error" when {
      "A number higher than 99 is supplied" in {
        val invalidSequenceNumber = 100
        val validationResult = SequenceValidation.validate(invalidSequenceNumber)
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe SequenceFormatError
      }
      "A number lower than 1 is supplied" in {
        val invalidSequenceNumber = 0
        val validationResult = SequenceValidation.validate(invalidSequenceNumber)
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe SequenceFormatError
      }
    }
  }
}
