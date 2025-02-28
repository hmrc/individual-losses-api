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

package v6.lossClaims.delete.def1

import common.errors.ClaimIdFormatError
import shared.controllers.validators.Validator
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.utils.UnitSpec
import v6.lossClaims.common.models.ClaimId
import v6.lossClaims.delete.def1.model.request.Def1_DeleteLossClaimRequestData
import v6.lossClaims.delete.model.request.DeleteLossClaimRequestData

class Def1_DeleteLossClaimValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino      = "AA123456A"
  private val invalidNino    = "badNino"
  private val validClaimId   = "AAZZ1234567890a"
  private val invalidClaimId = "not-a-claim-id"
  private val validTaxYear   = "2019-20"

  private val parsedNino    = Nino(validNino)
  private val parsedClaimId = ClaimId(validClaimId)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  private def validator(nino: String, claimId: String, taxYear: String): Validator[DeleteLossClaimRequestData] =
    new Def1_DeleteLossClaimValidator(nino, claimId, taxYear)

  "running a validation" should {
    "return the parsed request data" when {
      "passed a valid request" in {
        val result: Either[ErrorWrapper, DeleteLossClaimRequestData] = validator(validNino, validClaimId, validTaxYear).validateAndWrapResult()
        result shouldBe Right(
          Def1_DeleteLossClaimRequestData(parsedNino, parsedClaimId, parsedTaxYear)
        )
      }
    }

    "return NinoFormatError error" when {
      "passed an invalid nino" in {
        val result = validator(invalidNino, validClaimId, validTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return ClaimIdFormatError error" when {
      "passed an invalid claim ID" in {
        val result = validator(validNino, invalidClaimId, validTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, ClaimIdFormatError)
        )
      }
    }

    "return TaxYearFormatError error" when {
      "passed an incorrectly formatted taxYear" in {
        val result = validator(validNino, validClaimId, "202324").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }
    }

    "return a RuleTaxYearNotSupportedError error" when {
      "passed a taxYear before the minimum supported" in {
        validator(validNino, validClaimId, "2017-18").validateAndWrapResult() shouldBe
          Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }
    }

    "return a RuleTaxYearRangeInvalidError error" when {
      "passed a taxYear spanning an invalid tax year range" in {
        val result = validator(validNino, validClaimId, "2020-22").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }

    "return multiple errors" when {
      "passed a request with multiple errors" in {
        val result = validator(invalidNino, invalidClaimId, "invalidTaxYear").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(List(ClaimIdFormatError, NinoFormatError, TaxYearFormatError)))
        )
      }
    }
  }

}
