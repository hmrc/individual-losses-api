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
import v3.mocks.validators.MockCreateLossClaimValidator
import v3.models.domain.{LossClaim, Nino, TypeOfClaim, TypeOfLoss}
import v3.models.errors.{BadRequestError, ErrorWrapper, NinoFormatError, TaxYearFormatError}
import v3.models.requestData._

class CreateLossClaimParserSpec extends UnitSpec {

  private val nino = "AA123456B"
  private val taxYear = "2019-20"

  private val seRequestBodyJson = Json.parse(
    s"""
       |{
       |  "typeOfLoss" : "self-employment",
       |  "businessId" : "XAIS01234567890",
       |  "taxYearClaimedFor" : "$taxYear",
       |  "typeOfClaim" : "carry-forward"
       |}
     """.stripMargin
  )

  private val fPropRequestBodyJson = Json.parse(
    s"""
       |{
       |  "typeOfLoss" : "foreign-property",
       |  "businessId" : "XAIS01234567890",
       |  "taxYearClaimedFor" : "$taxYear",
       |  "typeOfClaim" : "carry-forward"
       |}
     """.stripMargin
  )

  private val ukPropNonFhlRequestBodyDowwnstreamJson = Json.parse(
    s"""
       |{
       |  "typeOfLoss" : "uk-property-non-fhl",
       |  "taxYearClaimedFor" : "$taxYear",
       |  "typeOfClaim" : "carry-forward",
       |  "businessId" : "X2IS12356589871"
       |}
     """.stripMargin
  )

  private val ukPropNonFhlRequestBodyJson = Json.parse(
    s"""
       |{
       |  "typeOfLoss" : "uk-property-non-fhl",
       |  "taxYearClaimedFor" : "$taxYear",
       |  "typeOfClaim" : "carry-forward",
       |  "businessId" : "X2IS12356589871"
       |}
     """.stripMargin
  )

  val seInputData: CreateLossClaimRawData =
    CreateLossClaimRawData(nino, AnyContentAsJson(seRequestBodyJson))
  val fPropInputData: CreateLossClaimRawData =
    CreateLossClaimRawData(nino, AnyContentAsJson(fPropRequestBodyJson))
  val ukPropInputData: CreateLossClaimRawData =
    CreateLossClaimRawData(nino, AnyContentAsJson(ukPropNonFhlRequestBodyDowwnstreamJson))
  val ukPropDownstreamData: CreateLossClaimRawData =
    CreateLossClaimRawData(nino, AnyContentAsJson(ukPropNonFhlRequestBodyJson))

  trait Test extends MockCreateLossClaimValidator {
    lazy val parser = new CreateLossClaimParser(mockValidator)
  }

  "parse" should {

    "return a request object" when {
      "valid request data is supplied for self employment" in new Test {
        MockValidator.validate(seInputData).returns(Nil)

        parser.parseRequest(seInputData) shouldBe
          Right(CreateLossClaimRequest(Nino(nino), LossClaim("2019-20", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, "XAIS01234567890")))
      }
      "valid request data is supplied for foreign property" in new Test {
        MockValidator.validate(fPropInputData).returns(Nil)

        parser.parseRequest(fPropInputData) shouldBe
          Right(CreateLossClaimRequest(Nino(nino), LossClaim("2019-20", TypeOfLoss.`foreign-property`, TypeOfClaim.`carry-forward`, "XAIS01234567890")))
      }
      "valid request data is supplied for uk property non fhl" in new Test {
        MockValidator.validate(ukPropDownstreamData).returns(Nil)

        parser.parseRequest(ukPropDownstreamData) shouldBe
          Right(CreateLossClaimRequest(
            Nino(nino), LossClaim("2019-20", TypeOfLoss.`uk-property-non-fhl`, TypeOfClaim.`carry-forward`, "X2IS12356589871")))
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockValidator.validate(seInputData)
          .returns(List(NinoFormatError))

        parser.parseRequest(seInputData) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockValidator.validate(seInputData)
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(seInputData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }
  }
}