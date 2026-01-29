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

package v7.lossesAndClaims.delete

import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.errors.{ErrorWrapper, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError}
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v7.lossesAndClaims.delete.model.request.DeleteLossesAndClaimsRequestData

class DeleteLossesAndClaimsValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private val validNino       = "AA123456A"
  private val validBusinessId = "X0IS12345678901"
  private val validTaxYear    = "2026-27"

  private implicit val correlationId: String = "1234"
  private val validatorFactory               = new DeleteLossesAndClaimsValidatorFactory

  private val parsedNino       = Nino(validNino)
  private val parsedBusinessId = BusinessId(validBusinessId)
  private val parsedTaxYear    = TaxYear.fromMtd(validTaxYear)

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear).validateAndWrapResult()
        result shouldBe Right(DeleteLossesAndClaimsRequestData(parsedNino, parsedBusinessId, parsedTaxYear))
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "given a tax year passed an unsupported tax year" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2025-26").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError)
        )
      }
    }

    "return RuleTaxYearRangeInvalid" when {
      "given a tax year range which isn't a single year" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2025-27").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError)
        )
      }
    }

  }

}
