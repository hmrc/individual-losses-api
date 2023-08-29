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

package v3.controllers.requestParsers.validators

import api.models.errors._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v3.models.request.amendLossClaimType.AmendLossClaimTypeRawData

class AmendLossClaimTypeValidatorSpec extends UnitSpec {

  private val validNino    = "AA123456A"
  private val validClaimId = "AAZZ1234567890a"

  private def requestBodyJson(claimType: String = "carry-forward") = AnyContentAsJson(Json.obj("typeOfClaim" -> claimType))

  val validator = new AmendLossClaimTypeValidator

  "Amend Loss Claim Validator" should {

    "return no errors" when {

      "all data is valid" in {
        validator.validate(AmendLossClaimTypeRawData(validNino, validClaimId, requestBodyJson())) shouldBe Nil
      }
    }

    "return NinoFormatError" when {
      "nino validation fails" in {
        validator.validate(AmendLossClaimTypeRawData("invalid", validClaimId, requestBodyJson())) shouldBe List(NinoFormatError)
      }
    }

    "return ClaimIdFormatError" when {
      "claimId validation fails" in {
        validator.validate(AmendLossClaimTypeRawData(validNino, "invalid", requestBodyJson())) shouldBe List(ClaimIdFormatError)
      }
    }

    "return RuleIncorrectOrEmptyBodyError" when {
      "an empty body is supplied" in {
        validator.validate(AmendLossClaimTypeRawData(validNino, validClaimId, AnyContentAsJson(JsObject.empty))) shouldBe List(
          RuleIncorrectOrEmptyBodyError)
      }

      "an incorrect body is supplied" in {
        validator.validate(
          AmendLossClaimTypeRawData(validNino, validClaimId, AnyContentAsJson(Json.obj("claimType" -> "carry-forward")))) shouldBe List(
          RuleIncorrectOrEmptyBodyError.copy(paths = Some(Seq("/typeOfClaim"))))
      }
    }

    "return TypeOfClaimFormatError" when {
      "type of claim validation fails" in {
        validator.validate(AmendLossClaimTypeRawData(validNino, validClaimId, requestBodyJson("invalid"))) shouldBe List(TypeOfClaimFormatError)
      }
    }
  }

}
