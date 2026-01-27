/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossClaim.delete

import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec

class DeleteLossClaimValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private val validNino       = "AA123456A"
  private val validBusinessId = "AAZZ1234567890a"
  private val validTaxYear    = "2019-20"

  private val validatorFactory = new DeleteLossClaimValidatorFactory

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear)
        result shouldBe a[DeleteLossClaimValidator]
      }
    }

  }

}
