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

import support.UnitSpec
import v3.mocks.validators.MockListLossClaimsValidator
import v3.models.domain.{TypeOfClaimLoss, Nino, TypeOfClaim}
import v3.models.errors.{BadRequestError, ErrorWrapper, LossIdFormatError, NinoFormatError}
import v3.models.requestData.{DownstreamTaxYear, ListLossClaimsRawData, ListLossClaimsRequest}

class ListLossClaimsParserSpec extends UnitSpec {

  private val nino       = "AA123456B"
  private val businessId = "XAIS01234567890"

  trait Test extends MockListLossClaimsValidator {
    lazy val parser = new ListLossClaimsParser(mockValidator)
  }

  "parse" when {
    "valid input" should {
      "work where parameters are provided" in new Test {
        val inputData: ListLossClaimsRawData =
          ListLossClaimsRawData(
            nino = nino,
            taxYearClaimedFor = Some("2017-18"),
            typeOfLoss = Some("uk-property-fhl"),
            businessId = Some(businessId),
            typeOfClaim = Some("carry-sideways")
          )

        MockValidator.validate(inputData) returns Nil

        parser.parseRequest(inputData) shouldBe
          Right(
            ListLossClaimsRequest(
              nino = Nino(nino),
              taxYearClaimedFor = Some(DownstreamTaxYear("2018")),
              typeOfLoss = Some(TypeOfClaimLoss.`uk-property-fhl`),
              businessId = Some(businessId),
              typeOfClaim = Some(TypeOfClaim.`carry-sideways`)
          ))
      }

      "map missing parameters to None" in new Test {
        val inputData: ListLossClaimsRawData =
          ListLossClaimsRawData(nino, None, None, None, None)

        MockValidator.validate(inputData) returns Nil

        parser.parseRequest(inputData) shouldBe
          Right(ListLossClaimsRequest(Nino(nino), None, None, None, None))
      }
    }

    "invalid input" should {
      // WLOG - validation is mocked
      val inputData = ListLossClaimsRawData("nino", None, None, None, None)

      "handle a single error" in new Test {
        MockValidator.validate(inputData) returns List(NinoFormatError)

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "handle multiple errors" in new Test {
        MockValidator.validate(inputData) returns List(NinoFormatError, LossIdFormatError)

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, LossIdFormatError))))
      }
    }
  }
}
