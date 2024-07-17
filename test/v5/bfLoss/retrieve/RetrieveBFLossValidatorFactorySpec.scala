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

package v5.bfLoss.retrieve

import api.controllers.validators.Validator
import api.models.domain.Nino
import api.models.errors._
import support.UnitSpec
import v5.bfLossClaims.retrieve.RetrieveBFLossValidatorFactory
import v5.bfLossClaims.retrieve.def1.model.request.Def1_RetrieveBFLossRequestData
import v5.bfLossClaims.retrieve.model._
import v5.bfLossClaims.retrieve.model.request.RetrieveBFLossRequestData

class RetrieveBFLossValidatorFactorySpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino     = "AA123456A"
  private val invalidNino   = "AA123456"
  private val validLossId   = "AAZZ1234567890a"
  private val invalidLossId = "AAZZ1234567890"

  private val parsedNino   = Nino(validNino)
  private val parsedLossId = LossId(validLossId)

  private val validatorFactory = new RetrieveBFLossValidatorFactory

  private def validator(nino: String, lossId: String): Validator[RetrieveBFLossRequestData] =
    validatorFactory.validator(nino, lossId)

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
