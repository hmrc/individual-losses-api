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

package api.endpoints.lossClaim.list.v4.request

import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import support.UnitSpec

class ListLossClaimsParserSpec extends UnitSpec {

  private val nino       = "AA123456B"
  private val businessId = "XAIS01234567890"

  implicit val correlationId: String = "X-123"

  trait Test extends MockListLossClaimsValidator {
    lazy val parser = new ListLossClaimsParser(mockValidator)
  }

  "parse" when {
    "valid input" should {
      "work where parameters are provided" in new Test {
        val inputData: ListLossClaimsRawData =
          ListLossClaimsRawData(
            nino = nino,
            taxYearClaimedFor = "2020-21",
            typeOfLoss = Some("uk-property-non-fhl"),
            businessId = Some(businessId),
            typeOfClaim = Some("carry-sideways")
          )

        MockValidator.validate(inputData) returns Nil

        parser.parseRequest(inputData) shouldBe
          Right(
            ListLossClaimsRequest(
              nino = Nino(nino),
              taxYearClaimedFor = TaxYear("2021"),
              typeOfLoss = Some(TypeOfLoss.`uk-property-non-fhl`),
              businessId = Some(businessId),
              typeOfClaim = Some(TypeOfClaim.`carry-sideways`)
            ))
      }

      "map missing parameters to None" in new Test {
        val inputData: ListLossClaimsRawData =
          ListLossClaimsRawData(nino, "2020-21", None, None, None)

        MockValidator.validate(inputData) returns Nil

        parser.parseRequest(inputData) shouldBe
          Right(ListLossClaimsRequest(Nino(nino), TaxYear("2021"), None, None, None))
      }
    }

    "invalid input" should {
      // WLOG - validation is mocked
      val inputData = ListLossClaimsRawData("nino", "2020-21", None, None, None)

      "handle a single error" in new Test {
        MockValidator.validate(inputData) returns List(NinoFormatError)

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }

      "handle multiple errors" in new Test {
        MockValidator.validate(inputData) returns List(NinoFormatError, LossIdFormatError)

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, LossIdFormatError))))
      }
    }
  }

}
