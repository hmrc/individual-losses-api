/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.controllers.requestParsers.validators

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v1.models.errors.{ClaimIdFormatError, NinoFormatError, RuleIncorrectOrEmptyBodyError, TypeOfClaimFormatError}
import v1.models.requestData.AmendLossClaimRawData

import scala.collection.immutable.Stream.Empty

class AmendLossClaimValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val invalidNino = "AA123456"
  private val validClaimId = "AAZZ1234567890a"
  private val invalidClaimId = "AAZZ1234567890"

  private def claimType(claimType: String) = AnyContentAsJson(Json.obj("typeOfClaim" -> claimType))

  val validator = new AmendLossClaimValidator

  "Amend Loss Claim Validator" should {

    "return no errors" when {

      "all data is valid" in {
        validator.validate(AmendLossClaimRawData(validNino, validClaimId, claimType("carry-forward"))) shouldBe Empty
      }
    }

    "return one error" when {

      "nino validation fails" in {
        validator.validate(AmendLossClaimRawData(invalidNino, validClaimId, claimType("carry-forward"))) shouldBe List(NinoFormatError)
      }

      "claimId validation fails" in {
        validator.validate(AmendLossClaimRawData(validNino, invalidClaimId, claimType("carry-forward"))) shouldBe List(ClaimIdFormatError)
      }

      "body validation fails" in {
        validator.validate(AmendLossClaimRawData(validNino, validClaimId, AnyContentAsJson(Json.obj()))) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }

      "type of claim validation fails" in {
        validator.validate(AmendLossClaimRawData(validNino, validClaimId, claimType("invalid"))) shouldBe List(TypeOfClaimFormatError)
      }
    }
  }
}
