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

package v4.controllers.validators

import common.errors.LossIdFormatError
import shared.controllers.validators.Validator
import shared.models.domain.Nino
import shared.models.errors._
import shared.utils.UnitSpec
import v4.models.domain.bfLoss.LossId
import v4.models.request.deleteBFLosses.DeleteBFLossRequestData

class DeleteBFLossValidatorFactorySpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino     = "AA123456A"
  private val invalidNino   = "badNino"
  private val validLossId   = "AAZZ1234567890a"
  private val invalidLossId = "not-a-loss-id"

  private val parsedNino   = Nino(validNino)
  private val parsedLossId = LossId(validLossId)

  private val validatorFactory = new DeleteBFLossValidatorFactory

  private def validator(nino: String, lossId: String): Validator[DeleteBFLossRequestData] = validatorFactory.validator(nino, lossId)

  "running a validation" should {
    "return the parsed request data" when {
      "given a valid request" in {
        val result = validator(validNino, validLossId).validateAndWrapResult()
        result shouldBe Right(
          DeleteBFLossRequestData(parsedNino, parsedLossId)
        )
      }
    }

    "return NinoFormatError error" when {
      "given an invalid nino" in {
        val result = validator(invalidNino, validLossId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return LossIdFormatError error" when {
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
