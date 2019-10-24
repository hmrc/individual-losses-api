/*
 * Copyright 2019 HM Revenue & Customs
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
import v1.models.domain.TypeOfClaim._
import v1.models.domain.{ TypeOfClaim, TypeOfLoss }
import v1.models.domain.TypeOfLoss._
import v1.models.errors.{ MtdError, RuleTypeOfClaimInvalid, TypeOfClaimFormatError }

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

  "checkClaimCompatibility" when {

    def testCompatibility(typeOfLoss: TypeOfLoss, typeOfClaim: TypeOfClaim, expectedErrors: List[MtdError]): Any =
      TypeOfClaimValidation.checkClaimCompatibility(typeOfClaim, typeOfLoss) shouldBe expectedErrors

    // Note: only uk-property-non-fhl and self-employment are supported for claims
    "typeOfLoss is 'uk-property-non-fhl'" in {
      testCompatibility(`uk-property-non-fhl`, `carry-forward`, List(RuleTypeOfClaimInvalid))
      testCompatibility(`uk-property-non-fhl`, `carry-sideways`, Nil)
      testCompatibility(`uk-property-non-fhl`, `carry-forward-to-carry-sideways`, Nil)
      testCompatibility(`uk-property-non-fhl`, `carry-sideways-fhl`, Nil)
    }

    "typeOfLoss is 'self-employment'" in {
      testCompatibility(`self-employment`, `carry-forward`, Nil)
      testCompatibility(`self-employment`, `carry-sideways`, Nil)
      testCompatibility(`self-employment`, `carry-sideways-fhl`, List(RuleTypeOfClaimInvalid))
      testCompatibility(`self-employment`, `carry-forward-to-carry-sideways`, List(RuleTypeOfClaimInvalid))
    }
  }
}
