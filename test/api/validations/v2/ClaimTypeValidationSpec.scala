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

package api.validations.v2

import api.endpoints.common.lossClaim.v2.domain.TypeOfClaim
import support.UnitSpec
import v2.models.errors.ClaimTypeFormatError

class ClaimTypeValidationSpec extends UnitSpec {

  "validate" should {

    "return no errors" when {

      "provided with a string of 'carry-sideways'" in {
        ClaimTypeValidation.validate("carry-sideways").isEmpty shouldBe true
      }
    }

    "return an error" when {

      "provided with an empty string" in {
        ClaimTypeValidation.validate("") shouldBe List(ClaimTypeFormatError)
      }

      "provided with a non-matching string" in {
        ClaimTypeValidation.validate("carry-forwar") shouldBe List(ClaimTypeFormatError)
      }
    }
  }

  "validateClaimIsCarrySideways" should {
    "return no errors" when {
      "provided with a string of 'carry-sideways'" in {
        ClaimTypeValidation.validateClaimIsCarrySideways(TypeOfClaim.`carry-sideways`).isEmpty shouldBe true
      }
    }
    "return an error" when {
      "provided with a non-matching string" in {
        ClaimTypeValidation.validateClaimIsCarrySideways(TypeOfClaim.`carry-forward`) shouldBe List(ClaimTypeFormatError)
      }
    }
  }

}
