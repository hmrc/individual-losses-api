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

package v3.validations

import api.validations.v3.TypeOfBFLossValidation
import support.UnitSpec
import v3.models.errors.TypeOfLossFormatError

class TypeOfBFLossValidationSpec extends UnitSpec {

  "validate" should {

    "return no errors" when {
      checkValid("self-employment")
      checkValid("self-employment-class4")
      checkValid("uk-property-fhl")
      checkValid("uk-property-non-fhl")
      checkValid("foreign-property")
      checkValid("foreign-property-fhl-eea")

      def checkValid(typeOfLoss: String): Unit =
        s"provided with a string of '$typeOfLoss'" in {
          TypeOfBFLossValidation.validate(typeOfLoss) shouldBe Nil
        }
    }

    "return a TypeOfLossFormatError" when {
      "provided with an unknown type of loss" in {
        TypeOfBFLossValidation.validate("invalid") shouldBe List(TypeOfLossFormatError)
      }
    }
  }
}
