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
import v1.models.errors.{ClaimIdFormatError, NinoFormatError}
import v1.models.requestData
import v1.models.requestData.DeleteLossClaimRawData

class DeleteLossClaimValidatorSpec extends UnitSpec{

  private val validNino = "AA123456A"
  private val validClaimId = "AAZZ1234567890a"

  val validator = new DeleteLossClaimValidator

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(DeleteLossClaimRawData(validNino, validClaimId)) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validator.validate(requestData.DeleteLossClaimRawData("badNino", validClaimId)) shouldBe
          List(NinoFormatError)
      }
    }

    "return LossIdFormatError error" when {
      "an invalid claim id is supplied" in {
        validator.validate(requestData.DeleteLossClaimRawData(validNino, "badClaimId")) shouldBe
          List(ClaimIdFormatError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(requestData.DeleteLossClaimRawData("badNino", "badClaimId")) shouldBe
          List(NinoFormatError, ClaimIdFormatError)
      }
    }
  }
}
