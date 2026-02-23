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

package v7.lossesAndClaims.retrieve

import shared.models.domain.{BusinessId, Nino, TaxYear}
import shared.models.errors.*
import shared.utils.UnitSpec
import v7.lossesAndClaims.retrieve.model.request.RetrieveLossesAndClaimsRequestData

class RetrieveLossesAndClaimsValidatorFactorySpec extends UnitSpec {

  private val validNino: String       = "AA123456A"
  private val validBusinessId: String = "X0IS12345678901"
  private val validTaxYear: String    = "2026-27"

  private implicit val correlationId: String = "1234"

  private val validatorFactory: RetrieveLossesAndClaimsValidatorFactory = new RetrieveLossesAndClaimsValidatorFactory

  private val parsedNino: Nino             = Nino(validNino)
  private val parsedBusinessId: BusinessId = BusinessId(validBusinessId)
  private val parsedTaxYear: TaxYear       = TaxYear.fromMtd(validTaxYear)

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validatorFactory.validator(validNino, validBusinessId, validTaxYear).validateAndWrapResult()
        result shouldBe Right(RetrieveLossesAndClaimsRequestData(parsedNino, parsedBusinessId, parsedTaxYear))
      }
    }

    "return NinoFormatError" when {
      "given a badly formatted nino" in {
        val result = validatorFactory.validator("validNino", validBusinessId, validTaxYear).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }
    }

    "return TaxYearFormatError" when {
      "given a badly formatted tax year" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2026").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }
    }

    "return BusinessIdFormatError" when {
      "given a badly formatted business ID" in {
        val result = validatorFactory.validator(validNino, "invalidBusinessId", validTaxYear).validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, BusinessIdFormatError))
      }
    }

    "return RuleTaxYearNotSupportedError" when {
      "given an unsupported tax year" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2025-26").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }
    }

    "return RuleTaxYearRangeInvalidError" when {
      "given a tax year with an invalid range" in {
        val result = validatorFactory.validator(validNino, validBusinessId, "2025-27").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }
  }

}
