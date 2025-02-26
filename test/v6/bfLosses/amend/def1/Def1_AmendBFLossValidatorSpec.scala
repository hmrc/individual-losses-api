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

package v6.bfLosses.amend.def1

import common.errors.LossIdFormatError
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec
import v6.bfLosses.amend.def1.model.request.{Def1_AmendBFLossRequestBody, Def1_AmendBFLossRequestData}
import v6.bfLosses.common.domain.LossId

class Def1_AmendBFLossValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino                    = "AA123456A"
  private val invalidNino                  = "BAD_NINO"
  private val validLossId                  = "AAZZ1234567890a"
  private val invalidLossId                = "AAZZ1234567890"
  private val validTaxYear                 = "2019-20"
  private val validTaxYearDownstreamFormat = "2020"
  private val invalidTaxYear               = "20201"
  private val parsedNino                   = Nino(validNino)
  private val parsedLossId                 = LossId(validLossId)
  private val parsedTaxYear                = TaxYear(validTaxYearDownstreamFormat)

  private def validator(nino: String, lossId: String, taxYear: String, body: JsValue) = new Def1_AmendBFLossValidator(nino, lossId, taxYear, body)

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validator(validNino, validLossId, validTaxYear, Json.obj("lossAmount" -> 3.0)).validateAndWrapResult()
        result shouldBe Right(
          Def1_AmendBFLossRequestData(parsedNino, parsedLossId, parsedTaxYear, Def1_AmendBFLossRequestBody(3.0))
        )
      }

      "given a valid nino and the minimum loss amount" in {
        val result = validator(validNino, validLossId, validTaxYear, Json.obj("lossAmount" -> 0.0)).validateAndWrapResult()
        result shouldBe Right(
          Def1_AmendBFLossRequestData(parsedNino, parsedLossId, parsedTaxYear, Def1_AmendBFLossRequestBody(0.0))
        )
      }

      "given a valid nino and the maximum loss amount" in {
        val result = validator(validNino, validLossId, validTaxYear, Json.obj("lossAmount" -> 99999999999.99)).validateAndWrapResult()
        result shouldBe Right(
          Def1_AmendBFLossRequestData(parsedNino, parsedLossId, parsedTaxYear, Def1_AmendBFLossRequestBody(99999999999.99))
        )
      }
    }
    "return a NinoFormatError" when {
      "given an invalid nino" in {
        val result = validator(invalidNino, validLossId, validTaxYear, Json.obj("lossAmount" -> 3.0)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return a LossIdFormatError" when {
      "given an invalid lossId" in {
        val result = validator(validNino, invalidLossId, validTaxYear, Json.obj("lossAmount" -> 3.0)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, LossIdFormatError)
        )
      }
    }

    "return an invalid TaxYear error " when {
      "given an invalid TaxYear" in {
        val result = validator(validNino, validLossId, invalidTaxYear, Json.obj("lossAmount" -> 3.0)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError)
        )
      }
    }

    "return a RuleIncorrectOrEmptyBodyError" when {
      "given a body without a lossAmount field" in {
        val result = validator(validNino, validLossId, validTaxYear, Json.obj()).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError)
        )
      }
    }

    "return a ValueFormatError" when {
      "given a lossAmount greater than 2 decimal places" in {
        val result = validator(validNino, validLossId, validTaxYear, Json.obj("lossAmount" -> 99999999999.999)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, ValueFormatError.forPathAndRange("/lossAmount", "0", "99999999999.99"))
        )
      }
    }

    "return multiple errors" when {
      "given a request with multiple errors" in {
        val result = validator(invalidNino, invalidLossId, invalidTaxYear, Json.obj("lossAmount" -> 3.0)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(List(LossIdFormatError, NinoFormatError, TaxYearFormatError)))
        )
      }
    }
  }

}
