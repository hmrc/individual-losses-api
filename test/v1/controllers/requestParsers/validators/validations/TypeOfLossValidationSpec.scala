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
import v1.models.errors.TypeOfLossFormatError

class TypeOfLossValidationSpec extends UnitSpec {

  "validate" should {

    "return no errors" when {

      "provided with a string of 'self-employment'" in {
        TypeOfLossValidation.validate("self-employment").isEmpty shouldBe true
      }

      "provided with a string of 'self-employment-class4'" in {
        TypeOfLossValidation.validate("self-employment-class4").isEmpty shouldBe true
      }

      "provided with a string of 'uk-property-fhl'" in {
        TypeOfLossValidation.validate("uk-property-fhl").isEmpty shouldBe true
      }

      "provided with a string of 'uk-property-non-fhl'" in {
        TypeOfLossValidation.validate("uk-property-non-fhl").isEmpty shouldBe true
      }
    }

    "return an error" when {

      "provided with an empty string" in {
        TypeOfLossValidation.validate("") shouldBe List(TypeOfLossFormatError)
      }

      "provided with a non-matching string" in {
        TypeOfLossValidation.validate("self-employment-a") shouldBe List(TypeOfLossFormatError)
      }
    }
  }
}
