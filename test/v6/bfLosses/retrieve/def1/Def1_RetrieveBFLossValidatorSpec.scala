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

package v6.bfLosses.retrieve.def1

import common.errors.LossIdFormatError
import shared.controllers.validators.Validator
import shared.models.domain.Nino
import shared.models.errors.*
import shared.utils.UnitSpec
import v6.bfLosses.common.domain.LossId
import v6.bfLosses.retrieve.def1.model.request.Def1_RetrieveBFLossRequestData
import v6.bfLosses.retrieve.model.request.RetrieveBFLossRequestData

class Def1_RetrieveBFLossValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino     = "AA123456A"
  private val invalidNino   = "BAD_NINO"
  private val validLossId   = "AAZZ1234567890a"
  private val invalidLossId = "AAZZ1234567890"

  private val parsedNino   = Nino(validNino)
  private val parsedLossId = LossId(validLossId)

  private def validator(nino: String, lossId: String): Validator[RetrieveBFLossRequestData] =
    new Def1_RetrieveBFLossValidator(nino, lossId)

  "RetrieveBFLossValidator" should {
    "return the parsed request data" when {
      "given a valid request" in {
        val result = validator(validNino, validLossId).validateAndWrapResult()
        result shouldBe Right(
          Def1_RetrieveBFLossRequestData(parsedNino, parsedLossId)
        )
      }
    }

    "return NinoFormatError" when {
      "given an invalid nino" in {
        val result = validator(invalidNino, validLossId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return LossIdFormatError" when {
      "given an invalid loss ID" in {
        val result = validator(validNino, invalidLossId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, LossIdFormatError)
        )
      }
    }

    "return multiple errors" when {
      "given a request with multiple errors" in {
        val result = validator(invalidNino, invalidLossId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(List(LossIdFormatError, NinoFormatError)))
        )
      }
    }
  }

}
