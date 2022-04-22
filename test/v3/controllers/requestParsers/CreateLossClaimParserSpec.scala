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

import api.endpoints.common.lossClaim.v3.domain.{ TypeOfClaim, TypeOfLoss }
import api.models.domain.Nino
import api.models.errors._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import support.UnitSpec
import v3.mocks.validators.MockCreateLossClaimValidator
import v3.models.request.createLossClaim.{ CreateLossClaimRawData, CreateLossClaimRequest, CreateLossClaimRequestBody }

class CreateLossClaimParserSpec extends UnitSpec {

  private val nino       = "AA123456B"
  private val taxYear    = "2019-20"
  private val businessId = "XAIS01234567890"

  private val requestBodyJson = Json.parse(
    s"""
       |{
       |  "typeOfLoss" : "self-employment",
       |  "businessId" : "$businessId",
       |  "taxYearClaimedFor" : "$taxYear",
       |  "typeOfClaim" : "carry-forward"
       |}
     """.stripMargin
  )

  private val rawData: CreateLossClaimRawData =
    CreateLossClaimRawData(nino, AnyContentAsJson(requestBodyJson))

  trait Test extends MockCreateLossClaimValidator {
    lazy val parser = new CreateLossClaimParser(mockValidator)
  }

  "parse" should {

    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockValidator.validate(rawData).returns(Nil)

        parser.parseRequest(rawData) shouldBe
          Right(
            CreateLossClaimRequest(Nino(nino),
                                   CreateLossClaimRequestBody(taxYear, TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, businessId)))
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockValidator
          .validate(rawData)
          .returns(List(NinoFormatError))

        parser.parseRequest(rawData) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockValidator
          .validate(rawData)
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(rawData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }
  }
}
