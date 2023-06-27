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

package api.endpoints.bfLoss.list.v4.request

import api.endpoints.bfLoss.domain.anyVersion.IncomeSourceType
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import support.UnitSpec

class ListBFLossesParserSpec extends UnitSpec {

  private val nino       = "AA123456B"
  private val rawTaxYear = "2017-18"
  private val taxYear    = TaxYear.fromMtd(rawTaxYear)
  private val businessId = "XAIS01234567890"

  private implicit val correlationId: String = "X-123"

  trait Test extends MockListBFLossesValidator {
    lazy val parser = new ListBFLossesParser(mockValidator)
  }

  "parse" when {
    "valid input" should {

      "convert uk-property-fhl to BFIncomeSourceType 04" in new Test {
        val inputData: ListBFLossesRawData =
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = rawTaxYear,
            typeOfLoss = Some("uk-property-fhl"),
            businessId = Some(businessId)
          )

        MockValidator
          .validate(inputData)
          .returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(
            ListBFLossesRequest(
              nino = Nino(nino),
              taxYearBroughtForwardFrom = TaxYear("2018"),
              incomeSourceType = Some(IncomeSourceType.`04`),
              businessId = Some(businessId)
            )
          )
      }

      "convert uk-property-non-fhl to BFIncomeSourceType 02" in new Test {
        val inputData: ListBFLossesRawData =
          ListBFLossesRawData(
            nino = nino,
            taxYearBroughtForwardFrom = rawTaxYear,
            typeOfLoss = Some("uk-property-non-fhl"),
            businessId = Some(businessId)
          )

        MockValidator
          .validate(inputData)
          .returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(
            ListBFLossesRequest(
              nino = Nino(nino),
              taxYearBroughtForwardFrom = TaxYear("2018"),
              incomeSourceType = Some(IncomeSourceType.`02`),
              businessId = Some(businessId))
          )
      }

      "map missing parameters to None" in new Test {
        val inputData: ListBFLossesRawData =
          ListBFLossesRawData(nino, rawTaxYear, None, None)

        MockValidator
          .validate(inputData)
          .returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(ListBFLossesRequest(Nino(nino), taxYear, None, None))
      }
    }

    "invalid input" should {
      val inputData = ListBFLossesRawData("nino", rawTaxYear, None, None)

      "handle a single error" in new Test {
        MockValidator
          .validate(inputData)
          .returns(List(NinoFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }

      "handle multiple errors" in new Test {
        MockValidator
          .validate(inputData)
          .returns(List(NinoFormatError, LossIdFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, LossIdFormatError))))
      }
    }
  }

}
