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

import api.validations.v2.BusinessIdValidation
import support.UnitSpec
import v2.models.errors.BusinessIdFormatError

class BusinessIdValidationSpec extends UnitSpec {

  val validId                 = "XAIS01234567890"
  val invalidIds: Seq[String] = Seq("AAIS01234567890", "X%IS01234567890", "XAAB01234567890", "XAIS0123456789", "XAIS012345678900", "XAIS012345A7890")

  "validate" should {

    "return no errors" when {

      "provided with a self employment loss with a valid id" in {
        BusinessIdValidation.validate(validId).isEmpty shouldBe true
      }

      "provided with a class 4 self employment loss with a valid id" in {
        BusinessIdValidation.validate(validId).isEmpty shouldBe true
      }
    }

    "return an error" when {

      invalidIds.foreach { badId =>
        s"provided with a self employment loss with an invalid id '$badId'" in {
          BusinessIdValidation.validate(badId) shouldBe List(BusinessIdFormatError)
        }
      }
    }
  }
}
