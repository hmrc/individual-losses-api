/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.controllers.requestParsers

import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v3.mocks.validators.MockAmendLossClaimTypeValidator
import v3.models.domain.Nino
import v3.models.domain.lossClaim.TypeOfClaim
import v3.models.errors.{BadRequestError, ErrorWrapper, NinoFormatError, RuleIncorrectOrEmptyBodyError}
import v3.models.request.amendLossClaimType.{AmendLossClaimTypeRawData, AmendLossClaimTypeRequest, AmendLossClaimTypeRequestBody}

class AmendLossClaimTypeParserSpec extends UnitSpec {

  private val nino      = "AA123456A"
  private val claimId   = "AAZZ1234567890a"
  private val lossClaim = TypeOfClaim.`carry-forward`

  val data: AmendLossClaimTypeRawData = AmendLossClaimTypeRawData(nino, claimId, AnyContentAsJson(Json.obj("typeOfClaim" -> lossClaim.toString)))

  trait Test extends MockAmendLossClaimTypeValidator {
    lazy val parser = new AmendLossClaimTypeParser(mockValidator)
  }

  "parse" should {
    "return an AmendBFLossRequest" when {
      "the validator returns no errors" in new Test {
        MockValidator.validate(data).returns(List())
        parser.parseRequest(data) shouldBe Right(AmendLossClaimTypeRequest(Nino(nino), claimId, AmendLossClaimTypeRequestBody(lossClaim)))
      }
    }
    "return a single error" when {
      "the validator returns a single error" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(None, NinoFormatError, None))
      }
    }
    "return multiple errors" when {
      "the validator returns multiple errors" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError, RuleIncorrectOrEmptyBodyError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, RuleIncorrectOrEmptyBodyError))))
      }
    }
  }
}