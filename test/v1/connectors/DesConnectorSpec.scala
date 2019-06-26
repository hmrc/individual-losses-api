/*
 * Copyright 2019 HM Revenue & Customs
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

package v1.connectors

import uk.gov.hmrc.domain.Nino
import v1.mocks.{MockAppConfig, MockHttpClient}
import v1.models.des.{AmendBFLossResponse, CreateBFLossResponse}
import v1.models.domain.{AmendBFLoss, BFLoss}
import v1.models.errors.{MultipleErrors, NinoFormatError, SingleError, TaxYearFormatError}
import v1.models.outcomes.DesResponse
import v1.models.requestData.{AmendBFLossRequest, CreateBFLossRequest}

import scala.concurrent.Future

class DesConnectorSpec extends ConnectorSpec {

  lazy val baseUrl  = "test-BaseUrl"
  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino    =  "AA123456A"
  val lossId  = "AAZZ1234567890a"

  class Test extends MockHttpClient with MockAppConfig {
    val connector: DesConnector = new DesConnector(http = mockHttpClient, appConfig = mockAppConfig)
    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "create BFLoss" when {
    val bfLoss = BFLoss("self-employment", Some("XKIS00000000988"), "2019-20", 256.78)
    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, CreateBFLossResponse(lossId)))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/brought-forward-losses/$nino", bfLoss)
          .returns(Future.successful(expected))

        createBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/brought-forward-losses/$nino", bfLoss)
          .returns(Future.successful(expected))

        createBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/brought-forward-losses/$nino", bfLoss)
          .returns(Future.successful(expected))

        createBFLossResult(connector) shouldBe expected
      }
    }

    def createBFLossResult(connector: DesConnector): DesOutcome[CreateBFLossResponse] =
      await(
        connector.createBFLoss(
          CreateBFLossRequest(
            nino = Nino(nino),
            bfLoss
          )))
  }

  "amend BFLoss" when {

    val amendBFLossResponse = AmendBFLossResponse(selfEmploymentId = Some("XKIS00000000988"), typeOfLoss = "INCOME",
      lossAmount = 500.13, taxYear = "2019-20")

    val amendBFLoss = AmendBFLoss(500.13)

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, amendBFLossResponse))

        MockedHttpClient
          .put(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", amendBFLoss)
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .put(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", amendBFLoss)
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockedHttpClient
          .put(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", amendBFLoss)
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    def amendBFLossResult(connector: DesConnector): DesOutcome[AmendBFLossResponse] =
      await(
        connector.amendBFLoss(
          AmendBFLossRequest(
            nino = Nino(nino),
            lossId = lossId,
            amendBFLoss
          )))
  }
}
