/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.list

import shared.utils.UnitSpec
import v6.lossClaims.list.def1.Def1_ListLossClaimsValidator

class ListLossClaimsValidatorFactorySpec extends UnitSpec {

  private val validNino        = "AA123456A"
  private val validTaxYear     = "2021-22"
  private val validLossType    = "self-employment"
  private val validBusinessId  = "XAIS01234567890"
  private val validTypeOfClaim = "carry-sideways"

  private val validatorFactory = new ListLossClaimsValidatorFactory

  "running validation" should {
    "return the parsed request data" when {
      "given a valid request" in {
        val result =
          validatorFactory.validator(validNino, validTaxYear, Some(validLossType), Some(validBusinessId), Some(validTypeOfClaim))
        result shouldBe a[Def1_ListLossClaimsValidator]
      }

      "given a valid request with no parameters" in {
        val result = validatorFactory.validator(validNino, validTaxYear, None, None, None)
        result shouldBe a[Def1_ListLossClaimsValidator]
      }
    }

  }

}
