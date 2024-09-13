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

package v5.lossClaims.delete.def1

import common.errors.ClaimIdFormatError
import shared.controllers.validators.Validator
import shared.models.domain.Nino
import shared.models.errors._
import shared.utils.UnitSpec
import v4.models.domain.lossClaim.ClaimId
import v5.lossClaims.delete.def1.model.request.Def1_DeleteLossClaimRequestData
import v5.lossClaims.delete.model.request.DeleteLossClaimRequestData

class Def1_DeleteLossClaimValidatorSpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino      = "AA123456A"
  private val invalidNino    = "badNino"
  private val validClaimId   = "AAZZ1234567890a"
  private val invalidClaimId = "not-a-claim-id"

  private val parsedNino    = Nino(validNino)
  private val parsedClaimId = ClaimId(validClaimId)

  private def validator(nino: String, claimId: String): Validator[DeleteLossClaimRequestData] = new Def1_DeleteLossClaimValidator(nino, claimId)

  "running a validation" should {
    "return the parsed request data" when {
      "given a valid request" in {
        val result: Either[ErrorWrapper, DeleteLossClaimRequestData] = validator(validNino, validClaimId).validateAndWrapResult()
        result shouldBe Right(
          Def1_DeleteLossClaimRequestData(parsedNino, parsedClaimId)
        )
      }
    }

    "return NinoFormatError error" when {
      "given an invalid nino" in {
        val result = validator(invalidNino, validClaimId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }
    }

    "return LossIdFormatError error" when {
      "given an invalid claim ID" in {
        val result = validator(validNino, invalidClaimId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, ClaimIdFormatError)
        )
      }
    }

    "return multiple errors" when {
      "given a request with multiple errors" in {
        val result = validator(invalidNino, invalidClaimId).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(List(ClaimIdFormatError, NinoFormatError)))
        )
      }
    }
  }

}
