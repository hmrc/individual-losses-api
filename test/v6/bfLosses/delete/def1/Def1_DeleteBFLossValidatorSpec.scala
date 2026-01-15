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

package v6.bfLosses.delete.def1

import common.errors.LossIdFormatError
import shared.controllers.validators.Validator
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors.*
import shared.utils.UnitSpec
import v6.bfLosses.common.domain.LossId
import v6.bfLosses.delete.def1.model.request.Def1_DeleteBFLossRequestData
import v6.bfLosses.delete.model.request.DeleteBFLossRequestData

class Def1_DeleteBFLossValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino           = "AA123456A"
  private val invalidNino         = "BAD_NINO"
  private val validLossId         = "AAZZ1234567890a"
  private val invalidLossId       = "not-a-loss-id"
  private val minimumvalidTaxYear = "2019-20"
  private val maximumvalidTaxYear = "2025-26"

  private val parsedNino    = Nino(validNino)
  private val parsedLossId  = LossId(validLossId)
  private val parsedTaxYear = TaxYear.fromMtd(minimumvalidTaxYear)

  private def validator(nino: String, lossId: String, taxYear: String): Validator[DeleteBFLossRequestData] =
    new Def1_DeleteBFLossValidator(nino, lossId, taxYear)

  "running a validation" should {
    "return the parsed request data" when {
      "passed a valid request with the earliest tax year" in {
        val result = validator(validNino, validLossId, minimumvalidTaxYear).validateAndWrapResult()
        result shouldBe Right(
          Def1_DeleteBFLossRequestData(parsedNino, parsedLossId, parsedTaxYear)
        )
      }

      "passed a valid request with the latest tax year" in {
        val result = validator(validNino, validLossId, maximumvalidTaxYear).validateAndWrapResult()
        result shouldBe Right(
          Def1_DeleteBFLossRequestData(parsedNino, parsedLossId, TaxYear.fromMtd(maximumvalidTaxYear))
        )
      }
    }

    "return a single error" when {
      "passed an invalid nino" in {
        val result = validator(invalidNino, validLossId, minimumvalidTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
      "passed an invalid loss ID" in {
        val result = validator(validNino, invalidLossId, minimumvalidTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, LossIdFormatError)
        )
      }
      "passed an incorrectly formatted taxYear" in {
        val result = validator(validNino, validLossId, "202324").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))

      }

      "passed a taxYear before the minimum supported" in {
        validator(validNino, validLossId, "2017-18").validateAndWrapResult() shouldBe
          Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }

      "passed a taxYear before the maximum supported" in {
        validator(validNino, validLossId, "2026-27").validateAndWrapResult() shouldBe
          Left(ErrorWrapper(correlationId, RuleTaxYearForVersionNotSupportedError))
      }

      "passed a taxYear spanning an invalid tax year range" in {
        val result = validator(validNino, validLossId, "2020-22").validateAndWrapResult()

        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }
    }

    "return multiple errors" when {
      "passed a request with multiple errors" in {
        val result = validator(invalidNino, invalidLossId, "invalidTaxYear").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(List(LossIdFormatError, NinoFormatError, TaxYearFormatError)))
        )
      }
    }
  }

}
