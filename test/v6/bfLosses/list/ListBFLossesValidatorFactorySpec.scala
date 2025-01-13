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

package v6.bfLosses.list

import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v6.bfLosses.list.def1.Def1_ListBFLossesValidator

class ListBFLossesValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private val validNino       = "AA123456A"
  private val validLossType   = "self-employment"
  private val validTaxYear    = "2021-22"
  private val validBusinessId = "XAIS01234567890"

  private val validatorFactory = new ListBFLossesValidatorFactory

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validatorFactory.validator(validNino, validTaxYear, Some(validLossType), Some(validBusinessId))
        result shouldBe a[Def1_ListBFLossesValidator]
      }
    }
  }

}
