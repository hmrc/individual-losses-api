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
import v1.models.requestData.RetrieveLossClaimRawData

class RetrieveLossClaimValidatorSpec extends UnitSpec {

  private val validNino     = "AA123456A"
  private val invalidNino   = "AA123456"
  private val validClaimId   = "AAZZ1234567890a"
  private val invalidClaimId = "AAZZ1234567890"

  private val rawData: (String, String) => RetrieveLossClaimRawData = (nino, id) => RetrieveLossClaimRawData(nino, id)

  val validator = new RetrieveLossClaimValidator

  "retrieve validation" should {
    "return no errors" when {
      "supplied with a valid nino and a valid loss amount" in {
        validator.validate(rawData(validNino, validClaimId)) shouldBe List()
      }
    }
    "return a FORMAT_NINO error" when {
      "the provided nino is invalid" in {
        validator.validate(rawData(invalidNino, validClaimId)) shouldBe List(NinoFormatError)
      }
    }
    "return a FORMAT_CLAIM_ID error" when {
      "the provided claimId is invalid" in {
        validator.validate(rawData(validNino, invalidClaimId)) shouldBe List(ClaimIdFormatError)
      }
    }
    "return multiple errors" when {
      "a request with multiple errors is provided" in {
        validator.validate(rawData(invalidNino, invalidClaimId)) shouldBe List(NinoFormatError, ClaimIdFormatError)
      }
    }
  }

}
