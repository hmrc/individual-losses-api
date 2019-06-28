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
import v1.models.errors.{SelfEmploymentIdFormatError, RulePropertySelfEmploymentId}

class SelfEmploymentIdValidationSpec extends UnitSpec {

  val validId = "XAIS01234567890"
  val invalidIds = Seq("AAIS01234567890", "X%IS01234567890", "XAAB01234567890", "XAIS0123456789", "XAIS012345678900", "XAIS012345A7890")

  "validate" should {

    "return no errors" when {

      "provided with an fhl property loss without an id" in {
        SelfEmploymentIdValidation.validate("uk-fhl-property", None).isEmpty shouldBe true
      }

      "provided with an other property loss without an id" in {
        SelfEmploymentIdValidation.validate("uk-other-property", None).isEmpty shouldBe true
      }

      "provided with a self employment loss with a valid id" in {
        SelfEmploymentIdValidation.validate("self-employment", Some(validId)).isEmpty shouldBe true
      }

      "provided with a class 4 self employment loss with a valid id" in {
        SelfEmploymentIdValidation.validate("self-employment-class4", Some(validId)).isEmpty shouldBe true
      }
    }

    "return an error" when {

      "provided with a property loss with a valid id" in {
        SelfEmploymentIdValidation.validate("uk-other-property", Some(validId)) shouldBe List(RulePropertySelfEmploymentId)
      }

      "provided with a self employment loss without any id" in {
        SelfEmploymentIdValidation.validate("self-employment", None) shouldBe List(SelfEmploymentIdFormatError)
      }

      invalidIds.foreach { badId =>
        s"provided with a self employment loss with an invalid id '$badId'" in {
          SelfEmploymentIdValidation.validate("self-employment", Some(badId)) shouldBe List(SelfEmploymentIdFormatError)
        }
      }
    }
  }
}
