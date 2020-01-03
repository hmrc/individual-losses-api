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

package v1.controllers.requestParsers.validators

import support.UnitSpec
import v1.models.errors._
import v1.models.requestData.RetrieveBFLossRawData

class RetrieveBFLossValidatorSpec extends UnitSpec {

  private val validNino     = "AA123456A"
  private val invalidNino   = "AA123456"
  private val validLossId   = "AAZZ1234567890a"
  private val invalidLossId = "AAZZ1234567890"

  private val retrieveBFLossRawData: (String, String) => RetrieveBFLossRawData = (nino, lossId) => RetrieveBFLossRawData(nino, lossId)

  val validator = new RetrieveBFLossValidator

  "retrieve validation" should {
    "return no errors" when {
      "supplied with a valid nino and a valid loss amount" in {
        validator.validate(retrieveBFLossRawData(validNino, validLossId)) shouldBe empty
      }
    }
    "return a FORMAT_NINO error" when {
      "the provided nino is invalid" in {
        validator.validate(retrieveBFLossRawData(invalidNino, validLossId)) shouldBe List(NinoFormatError)
      }
    }
    "return a FORMAT_LOSS_ID error" when {
      "the provided lossId is invalid" in {
        validator.validate(retrieveBFLossRawData(validNino, invalidLossId)) shouldBe List(LossIdFormatError)
      }
    }
    "return multiple errors" when {
      "a request with multiple errors is provided" in {
        validator.validate(retrieveBFLossRawData(invalidNino, invalidLossId)) shouldBe List(NinoFormatError, LossIdFormatError)
      }
    }
  }

}
