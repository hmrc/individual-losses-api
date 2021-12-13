/*
 * Copyright 2021 HM Revenue & Customs
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
import v3.mocks.validators.MockListBFLossesValidator
import v3.models.downstream.IncomeSourceType
import v3.models.domain.Nino
import v3.models.errors.{BadRequestError, ErrorWrapper, LossIdFormatError, NinoFormatError}
import v3.models.requestData.{DownstreamTaxYear, ListBFLossesRawData, ListBFLossesRequest}

class ListBFLossesParserSpec extends UnitSpec {

  private val nino        = "AA123456B"
  private val taxYear     = "2017-18"
  private val businessId  = "XAIS01234567890"

  trait Test extends MockListBFLossesValidator {
    lazy val parser = new ListBFLossesParser(mockValidator)
  }

  "parse" when {
    "valid input" should {

      "convert uk-property-fhl to incomeSourceType 04" in new Test {
        val inputData: ListBFLossesRawData =
          ListBFLossesRawData(
            nino = nino,
            taxYear = Some(taxYear),
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
                taxYear = Some(DownstreamTaxYear("2018")),
                incomeSourceType = Some(IncomeSourceType.`04`),
                businessId = Some(businessId)
              )
          )
      }

      "convert uk-property-non-fhl to incomeSourceType 02" in new Test {
        val inputData: ListBFLossesRawData =
          ListBFLossesRawData(
            nino = nino,
            taxYear = Some(taxYear),
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
              taxYear = Some(DownstreamTaxYear("2018")),
              incomeSourceType = Some(IncomeSourceType.`02`),
              businessId = Some(businessId))
          )
      }

      "map missing parameters to None" in new Test {
        val inputData: ListBFLossesRawData =
          ListBFLossesRawData(nino, None, None, None)

        MockValidator
          .validate(inputData)
          .returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(ListBFLossesRequest(Nino(nino), None, None, None))
      }
    }

    "invalid input" should {
      // WLOG - validation is mocked
      val inputData = ListBFLossesRawData("nino", None, None, None)

      "handle a single error" in new Test {
        MockValidator
          .validate(inputData)
          .returns(List(NinoFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "handle multiple errors" in new Test {
        MockValidator
          .validate(inputData)
          .returns(List(NinoFormatError, LossIdFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, LossIdFormatError))))
      }
    }
  }
}