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

package v2.connectors

import api.connectors.DownstreamOutcome
import api.connectors.v2.BFLossConnector
import api.endpoints.amendBFLoss.common.request.AmendBFLossRequestBody
import api.endpoints.amendBFLoss.v2.domain
import api.endpoints.amendBFLoss.v2.domain.BFLoss
import api.endpoints.amendBFLoss.v2.request.AmendBFLossRequest
import api.endpoints.createBFLoss.v2.model.downstream.CreateBFLossResponse
import api.endpoints.createBFLoss.v2.model.request.CreateBFLossRequest
import api.mocks.v2.MockHttpClient
import api.models.domain.Nino
import api.models.domain.lossClaim.v2.TypeOfLoss
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import mocks.MockAppConfig
import uk.gov.hmrc.http.HeaderCarrier
import v2.models.des._
import v2.models.requestData._

import scala.concurrent.Future

class BFLossConnectorSpec extends ConnectorSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  class Test extends MockHttpClient with MockAppConfig {

    val connector: BFLossConnector = new BFLossConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.desBaseUrl returns baseUrl
    MockAppConfig.desToken returns "des-token"
    MockAppConfig.desEnvironment returns "des-environment"
    MockAppConfig.desEnvironmentHeaders returns Some(allowedDesHeaders)
  }

  "create BFLoss" when {

    val bfLoss: BFLoss = domain.BFLoss(TypeOfLoss.`self-employment`, Some("XKIS00000000988"), "2019-20", 256.78)

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))

    val requiredDesHeadersPost: Seq[(String, String)] = requiredDesHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(ResponseWrapper(correlationId, CreateBFLossResponse(lossId)))

        MockHttpClient
          .post(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            config = dummyDesHeaderCarrierConfig,
            body = bfLoss,
            requiredHeaders = requiredDesHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        createBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .post(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            config = dummyDesHeaderCarrierConfig,
            body = bfLoss,
            requiredHeaders = requiredDesHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        createBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockHttpClient
          .post(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            config = dummyDesHeaderCarrierConfig,
            body = bfLoss,
            requiredHeaders = requiredDesHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        createBFLossResult(connector) shouldBe expected
      }
    }

    def createBFLossResult(connector: BFLossConnector): DownstreamOutcome[CreateBFLossResponse] =
      await(
        connector.createBFLoss(
          CreateBFLossRequest(
            nino = Nino(nino),
            bfLoss
          )))
  }

  "amend BFLoss" when {

    val amendBFLossResponse: BFLossResponse = BFLossResponse(
      businessId = Some("XKIS00000000988"),
      typeOfLoss = TypeOfLoss.`self-employment`,
      lossAmount = 500.13,
      taxYear = "2019-20",
      lastModified = "2018-07-13T12:13:48.763Z"
    )

    val amendBFLoss: AmendBFLossRequestBody = AmendBFLossRequestBody(500.13)

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))

    val requiredDesHeadersPut: Seq[(String, String)] = requiredDesHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(ResponseWrapper(correlationId, amendBFLossResponse))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyDesHeaderCarrierConfig,
            body = amendBFLoss,
            requiredHeaders = requiredDesHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyDesHeaderCarrierConfig,
            body = amendBFLoss,
            requiredHeaders = requiredDesHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyDesHeaderCarrierConfig,
            body = amendBFLoss,
            requiredHeaders = requiredDesHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    def amendBFLossResult(connector: BFLossConnector): DownstreamOutcome[BFLossResponse] =
      await(
        connector.amendBFLoss(
          AmendBFLossRequest(
            nino = Nino(nino),
            lossId = lossId,
            amendBFLoss
          )))
  }

  "delete BFLoss" when {

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(ResponseWrapper(correlationId, ()))

        MockHttpClient
          .delete(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        deleteBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .delete(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        deleteBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, LossIdFormatError))))

        MockHttpClient
          .delete(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        deleteBFLossResult(connector) shouldBe expected
      }
    }

    def deleteBFLossResult(connector: BFLossConnector): DownstreamOutcome[Unit] =
      await(
        connector.deleteBFLoss(
          DeleteBFLossRequest(
            nino = Nino(nino),
            lossId = lossId
          )))
  }

  "retrieveBFLoss" should {

    val retrieveResponse: BFLossResponse = BFLossResponse(
      businessId = Some("fakeId"),
      typeOfLoss = TypeOfLoss.`self-employment`,
      lossAmount = 2000.25,
      taxYear = "2018-19",
      lastModified = "dateString"
    )

    def retrieveBFLossResult(connector: BFLossConnector): DownstreamOutcome[BFLossResponse] = {
      await(
        connector.retrieveBFLoss(
          RetrieveBFLossRequest(
            nino = Nino(nino),
            lossId = lossId
          )
        )
      )
    }

    "return a successful response and correlationId" when {

      "provided with a valid request" in new Test {
        val expected = Left(ResponseWrapper(correlationId, retrieveResponse))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        retrieveBFLossResult(connector) shouldBe expected
      }
    }

    "return an unsuccessful response" when {

      "provided with a single error" in new Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        retrieveBFLossResult(connector) shouldBe expected
      }

      "provided with multiple errors" in new Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, LossIdFormatError))))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        retrieveBFLossResult(connector) shouldBe expected
      }
    }
  }

  "listBFLosses" should {

    def listBFLossesResult(connector: BFLossConnector,
                           taxYear: Option[DesTaxYear] = None,
                           incomeSourceType: Option[IncomeSourceType] = None,
                           businessId: Option[String] = None): DownstreamOutcome[ListBFLossesResponse[BFLossId]] = {
      await(
        connector.listBFLosses(
          ListBFLossesRequest(
            nino = Nino(nino),
            taxYear = taxYear,
            incomeSourceType = incomeSourceType,
            businessId = businessId
          )))
    }

    "return a successful response" when {

      "provided with no parameters" in new Test {
        val expected = Left(ResponseWrapper(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            parameters = Seq(),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listBFLossesResult(connector) shouldBe expected
      }

      "provided with a tax year parameter" in new Test {
        val expected = Left(ResponseWrapper(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            parameters = Seq(("taxYear", "2019")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listBFLossesResult(connector = connector, taxYear = Some(DesTaxYear("2019"))) shouldBe expected
      }

      "provided with a income source id parameter" in new Test {
        val expected = Left(ResponseWrapper(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            parameters = Seq(("incomeSourceId", "testId")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listBFLossesResult(connector = connector, businessId = Some("testId")) shouldBe expected
      }

      "provided with a income source type parameter" in new Test {
        val expected = Left(ResponseWrapper(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            parameters = Seq(("incomeSourceType", "02")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listBFLossesResult(connector = connector, incomeSourceType = Some(IncomeSourceType.`02`)) shouldBe expected
      }

      "provided with all parameters" in new Test {
        val expected = Left(ResponseWrapper(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            parameters = Seq(("taxYear", "2019"), ("incomeSourceId", "testId"), ("incomeSourceType", "01")),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listBFLossesResult(connector = connector,
                           taxYear = Some(DesTaxYear("2019")),
                           businessId = Some("testId"),
                           incomeSourceType = Some(IncomeSourceType.`01`)) shouldBe expected
      }
    }

    "return an unsuccessful response" when {

      "provided with a single error" in new Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            parameters = Seq(),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listBFLossesResult(connector) shouldBe expected
      }

      "provided with multiple errors" in new Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, LossIdFormatError))))

        MockHttpClient
          .parameterGet(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            parameters = Seq(),
            config = dummyDesHeaderCarrierConfig,
            requiredHeaders = requiredDesHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listBFLossesResult(connector) shouldBe expected
      }
    }
  }
}
