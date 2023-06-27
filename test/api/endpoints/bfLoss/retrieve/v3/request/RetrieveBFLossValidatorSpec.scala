/*
 * Copyright 2023 HM Revenue & Customs
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

package api.endpoints.bfLoss.retrieve.v3.request

import api.models.errors._
import support.UnitSpec

class RetrieveBFLossValidatorSpec extends UnitSpec {

  private val validNino     = "AA123456A"
  private val invalidNino   = "AA123456"
  private val validLossId   = "AAZZ1234567890a"
  private val invalidLossId = "AAZZ1234567890"

  val validator = new RetrieveBFLossValidator

  "RetrieveBFLossValidator" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(RetrieveBFLossRawData(validNino, validLossId)) shouldBe Nil
      }
    }

    "return NinoFormatError" when {
      "the provided nino is invalid" in {
        validator.validate(RetrieveBFLossRawData(invalidNino, validLossId)) shouldBe List(NinoFormatError)
      }
    }

    "return LossIdFormatError" when {
      "the provided loss ID is invalid" in {
        validator.validate(RetrieveBFLossRawData(validNino, invalidLossId)) shouldBe List(LossIdFormatError)
      }
    }

    "return multiple errors" when {
      "a request with multiple errors is provided" in {
        validator.validate(RetrieveBFLossRawData(invalidNino, invalidLossId)) shouldBe List(NinoFormatError, LossIdFormatError)
      }
    }
  }

}
