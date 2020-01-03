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
import v1.models.errors.ClaimIdFormatError
import v1.models.utils.JsonErrorValidators

class ClaimIdValidationSpec extends UnitSpec with JsonErrorValidators {

  private val validClaimId   = "AAZZ1234567890a"
  private val invalidClaimId = "AAZZ1234567890"

  "ClaimIdValidation.validate" should {
    "return an empty list" when {
      "passed a valid claimId" in {
        ClaimIdValidation.validate(validClaimId) shouldBe empty
      }
    }
    "return a non-empty list" when {
      "passed an invalid claimId" in {
        ClaimIdValidation.validate(invalidClaimId) shouldBe List(ClaimIdFormatError)
      }
    }
  }

}
