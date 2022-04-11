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

import api.models.domain.lossClaim.v3.TypeOfClaim
import api.models.domain.{DownstreamTaxYear, Nino}
import api.models.errors._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v3.mocks.validators.MockAmendLossClaimsOrderValidator
import v3.models.request.amendLossClaimsOrder.{AmendLossClaimsOrderRawData, AmendLossClaimsOrderRequest, AmendLossClaimsOrderRequestBody, Claim}

class AmendLossClaimsOrderParserSpec extends UnitSpec {

  private val nino        = "AA123456A"
  private val typeOfClaim = "carry-sideways"
  private val taxYear     = "2020-21"
  private val claim       = Json.obj("claimId" -> "1234568790ABCDE", "sequence" -> 1)

  val data: AmendLossClaimsOrderRawData =
    AmendLossClaimsOrderRawData(nino, taxYear, AnyContentAsJson(Json.obj("typeOfClaim" -> typeOfClaim, "listOfLossClaims" -> Seq(claim))))

  trait Test extends MockAmendLossClaimsOrderValidator {
    lazy val parser = new AmendLossClaimsOrderParser(mockValidator)
  }

  "parse" should {
    "return an AmendLossClaimsOrderRequest" when {
      "the validator returns no errors and a tax year is supplied" in new Test {
        MockValidator.validate(data).returns(List())

        parser.parseRequest(data) shouldBe {
          Right(
            AmendLossClaimsOrderRequest(Nino(nino),
                                        DownstreamTaxYear.fromMtd(taxYear),
                                        AmendLossClaimsOrderRequestBody(TypeOfClaim.`carry-sideways`, Seq(Claim("1234568790ABCDE", 1)))))
        }
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
        MockValidator.validate(data).returns(List(NinoFormatError, ClaimIdFormatError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, ClaimIdFormatError))))
      }
    }
  }
}
