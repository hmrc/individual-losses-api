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

package api.endpoints.lossClaim.amendOrder.v3.request

import api.endpoints.lossClaim.amendOrder.v3.model.Claim
import api.endpoints.lossClaim.domain.v3.TypeOfClaim
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec

class AmendLossClaimsOrderParserSpec extends UnitSpec {

  private val nino        = "AA123456A"
  private val typeOfClaim = "carry-sideways"
  private val taxYear     = "2020-21"
  private val claim       = Json.obj("claimId" -> "1234568790ABCDE", "sequence" -> 1)

  implicit val correlationId: String = "X-123"

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
                                        TaxYear.fromMtd(taxYear),
                                        AmendLossClaimsOrderRequestBody(TypeOfClaim.`carry-sideways`, Seq(Claim("1234568790ABCDE", 1)))))
        }
      }
    }
    "return a single error" when {
      "the validator returns a single error" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }
    }
    "return multiple errors" when {
      "the validator returns multiple errors" in new Test {
        MockValidator.validate(data).returns(List(NinoFormatError, ClaimIdFormatError))
        parser.parseRequest(data) shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, ClaimIdFormatError))))
      }
    }
  }
}
