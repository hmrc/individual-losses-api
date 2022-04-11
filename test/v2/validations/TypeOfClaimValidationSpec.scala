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

package v2.validations

import api.models.domain.lossClaim.v2.TypeOfClaim._
import api.models.domain.lossClaim.v2.TypeOfLoss._
import api.models.errors.TypeOfClaimFormatError
import api.validations.v2.TypeOfClaimValidation
import support.UnitSpec
import v2.models.errors.RuleTypeOfClaimInvalid

class TypeOfClaimValidationSpec extends UnitSpec {

  "validate" should {

    "return no errors" when {

      "provided with a string of 'carry-forward'" in {
        TypeOfClaimValidation.validate("carry-forward").isEmpty shouldBe true
      }

      "provided with a string of 'carry-sideways'" in {
        TypeOfClaimValidation.validate("carry-sideways").isEmpty shouldBe true
      }

      "provided with a string of 'carry-forward-to-carry-sideways'" in {
        TypeOfClaimValidation.validate("carry-forward-to-carry-sideways").isEmpty shouldBe true
      }

      "provided with a string of 'carry-sideways-fhl'" in {
        TypeOfClaimValidation.validate("carry-sideways-fhl").isEmpty shouldBe true
      }
    }

    "return an error" when {

      "provided with an empty string" in {
        TypeOfClaimValidation.validate("") shouldBe List(TypeOfClaimFormatError)
      }

      "provided with a non-matching string" in {
        TypeOfClaimValidation.validate("carry-forwar") shouldBe List(TypeOfClaimFormatError)
      }
    }
  }

  "checkClaim" should {

    "return no errors when typeOfLoss is 'uk-property-non-fhl' and" when {

      "provided with a string of 'carry-sideways'" in {
        TypeOfClaimValidation.checkClaim(`carry-sideways`, `uk-property-non-fhl`).isEmpty shouldBe true
      }

      "provided with a string of 'carry-forward-to-carry-sideways'" in {
        TypeOfClaimValidation.checkClaim(`carry-forward-to-carry-sideways`, `uk-property-non-fhl`).isEmpty shouldBe true
      }

      "provided with a string of 'carry-sideways-fhl'" in {
        TypeOfClaimValidation.checkClaim(`carry-sideways-fhl`, `uk-property-non-fhl`).isEmpty shouldBe true
      }
    }

    "return no errors when typeOfLoss is 'self-employment' and" when {

      "typeOfClaim is 'carry-forward'" in {
        TypeOfClaimValidation.checkClaim(`carry-forward`, `self-employment`).isEmpty shouldBe true
      }

      "provided with a string of 'carry-sideways'" in {
        TypeOfClaimValidation.checkClaim(`carry-sideways`, `self-employment`).isEmpty shouldBe true
      }
    }

    "return no errors when typeOfLoss is 'foreign-property' and" when {

      "typeOfClaim is 'carry-forward-to-carry-sideways'" in {
        TypeOfClaimValidation.checkClaim(`carry-forward-to-carry-sideways`, `foreign-property`).isEmpty shouldBe true
      }

      "provided with a string of 'carry-sideways'" in {
        TypeOfClaimValidation.checkClaim(`carry-sideways`, `foreign-property`).isEmpty shouldBe true
      }
      "provided with a string of 'carry-sideways-fhl'" in {
        TypeOfClaimValidation.checkClaim(`carry-sideways-fhl`, `foreign-property`).isEmpty shouldBe true
      }
    }

    "return TypeOfClaimFormatError when typeOfLoss is 'self-employment' and" when {

      "typeOfClaim is 'carry-sideways-fhl" in {
        TypeOfClaimValidation.checkClaim(`carry-sideways-fhl`, `self-employment`) shouldBe List(RuleTypeOfClaimInvalid)
      }

      "typeOfClaim is 'carry-forward-to-carry-sideways" in {
        TypeOfClaimValidation.checkClaim(`carry-forward-to-carry-sideways`, `self-employment`) shouldBe List(RuleTypeOfClaimInvalid)
      }

    }

    "return typeOfClaimFormatError when typeOfLoss is 'uk-property-non-fhl' and" when {

      "typeOfClaim is 'carry-forward'" in {
        TypeOfClaimValidation.checkClaim(`carry-forward`, `uk-property-non-fhl`) shouldBe List(RuleTypeOfClaimInvalid)
      }

      "return typeOfClaimFormatError when typeOfLoss is 'foreign-property' and" when {

        "typeOfClaim is 'carry-forward'" in {
          TypeOfClaimValidation.checkClaim(`carry-forward`, `foreign-property`) shouldBe List(RuleTypeOfClaimInvalid)
        }
      }
    }
  }
}
