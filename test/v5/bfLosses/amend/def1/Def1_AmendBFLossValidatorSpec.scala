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

package v5.bfLosses.amend.def1

import api.models.domain.Nino
import api.models.errors._
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v5.bfLosses.amend.def1.model.request.{Def1_AmendBFLossRequestBody, Def1_AmendBFLossRequestData}
import v5.bfLosses.common.domain.LossId

class Def1_AmendBFLossValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino     = "AA123456A"
  private val invalidNino   = "BAD_NINO"
  private val validLossId   = "AAZZ1234567890a"
  private val invalidLossId = "AAZZ1234567890"

  private val parsedNino   = Nino(validNino)
  private val parsedLossId = LossId(validLossId)

  private def validator(nino: String, lossId: String, body: JsValue) = new Def1_AmendBFLossValidator(nino, lossId, body)

  "running a validation" should {
    "return the parsed domain object" when {
      "given a valid request" in {
        val result = validator(validNino, validLossId, Json.obj("lossAmount" -> 3.0)).validateAndWrapResult()
        result shouldBe Right(
          Def1_AmendBFLossRequestData(parsedNino, parsedLossId, Def1_AmendBFLossRequestBody(3.0))
        )
      }

      "given a valid nino and the minimum loss amount" in {
        val result = validator(validNino, validLossId, Json.obj("lossAmount" -> 0.0)).validateAndWrapResult()
        result shouldBe Right(
          Def1_AmendBFLossRequestData(parsedNino, parsedLossId, Def1_AmendBFLossRequestBody(0.0))
        )
      }

      "given a valid nino and the maximum loss amount" in {
        val result = validator(validNino, validLossId, Json.obj("lossAmount" -> 99999999999.99)).validateAndWrapResult()
        result shouldBe Right(
          Def1_AmendBFLossRequestData(parsedNino, parsedLossId, Def1_AmendBFLossRequestBody(99999999999.99))
        )
      }
    }
    "return a NinoFormatError" when {
      "given an invalid nino" in {
        val result = validator(invalidNino, validLossId, Json.obj("lossAmount" -> 3.0)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return a LossIdFormatError" when {
      "given an invalid lossId" in {
        val result = validator(validNino, invalidLossId, Json.obj("lossAmount" -> 3.0)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, LossIdFormatError)
        )
      }
    }

    "return a RuleIncorrectOrEmptyBodyError" when {
      "given a body without a lossAmount field" in {
        val result = validator(validNino, validLossId, Json.obj()).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError)
        )
      }
    }

    "return a ValueFormatError" when {
      "given a lossAmount greater than 2 decimal places" in {
        val result = validator(validNino, validLossId, Json.obj("lossAmount" -> 99999999999.999)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, ValueFormatError.forPathAndRange("/lossAmount", "0", "99999999999.99"))
        )
      }
    }

    "return multiple errors" when {
      "given a request with multiple errors" in {
        val result = validator(invalidNino, invalidLossId, Json.obj("lossAmount" -> 3.0)).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(List(LossIdFormatError, NinoFormatError)))
        )
      }
    }
  }

}
