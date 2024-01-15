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

package v4.controllers.validators.resolvers

import api.models.errors.ClaimIdFormatError
import cats.data.Validated.{Invalid, Valid}
import support.UnitSpec
import v4.models.domain.lossClaim.ClaimId

class ResolveLossClaimIdSpec extends UnitSpec {

  private val validClaimId   = "AAZZ1234567890a"
  private val invalidClaimId = "AAZZ1234567890"

  "ResolveClaimId" should {
    "return the resolved ClaimId" when {
      "given a valid claimId" in {
        val result = ResolveLossClaimId(validClaimId)
        result shouldBe Valid(ClaimId(validClaimId))
      }
    }

    "return a ClaimIdFormatError" when {
      "given an invalid claimId" in {
        val result = ResolveLossClaimId(invalidClaimId)
        result shouldBe Invalid(List(ClaimIdFormatError))
      }
    }
  }

}
