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

package v5.bfLosses.list

import api.models.utils.JsonErrorValidators
import support.UnitSpec
import v5.bfLosses.list.ListBFLossesValidatorFactory
import v5.bfLosses.list.def1.Def1_ListBFLossesValidator

class ListBFLossesValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private val validNino        = "AA123456A"
  private val validLossType     = "self-employment"
  private val validTaxYear = "2021-22"
  private val validBusinessId = "XAIS01234567890"

  private val validatorFactory = new ListBFLossesValidatorFactory

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request with both optional fields" in {
        val result = validatorFactory.validator(validNino, validTaxYear, Some(validLossType), Some(validBusinessId))
        result shouldBe a[Def1_ListBFLossesValidator]
      }
      "given a valid request with one optional field of businessId" in {
        val result = validatorFactory.validator(validNino, validTaxYear, None, Some(validBusinessId))
        result shouldBe a[Def1_ListBFLossesValidator]
      }
      "given a valid request with one optional field of loss type" in {
        val result = validatorFactory.validator(validNino, validTaxYear, Some(validLossType), None)
        result shouldBe a[Def1_ListBFLossesValidator]
      }
      "given a valid request with no optional fields" in {
        val result = validatorFactory.validator(validNino, validTaxYear, None, None)
        result shouldBe a[Def1_ListBFLossesValidator]
      }

    }

  }

}
