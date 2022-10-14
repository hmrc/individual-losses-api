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

package api.endpoints.bfLoss.connector.v3

import api.connectors.{ ConnectorSpec, DownstreamOutcome }
import api.endpoints.bfLoss.amend.anyVersion.request.AmendBFLossRequestBody
import api.endpoints.bfLoss.amend.v3.request.AmendBFLossRequest
import api.endpoints.bfLoss.amend.v3.response.AmendBFLossResponse
import api.endpoints.bfLoss.create.v3.request.{ CreateBFLossRequest, CreateBFLossRequestBody }
import api.endpoints.bfLoss.create.v3.response.CreateBFLossResponse
import api.endpoints.bfLoss.delete.v3.request.DeleteBFLossRequest
import api.endpoints.bfLoss.domain.v3.{ IncomeSourceType, TypeOfLoss }
import api.endpoints.bfLoss.list.v3.request.ListBFLossesRequest
import api.endpoints.bfLoss.list.v3.response.{ ListBFLossesItem, ListBFLossesResponse }
import api.endpoints.bfLoss.retrieve.v3.request.RetrieveBFLossRequest
import api.endpoints.bfLoss.retrieve.v3.response.RetrieveBFLossResponse
import api.mocks.MockHttpClient
import api.models.ResponseWrapper
import api.models.domain.{ TaxYear, Nino }
import api.models.errors._
import config.MockAppConfig
import org.scalamock.handlers.CallHandler
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class BFLossConnectorSpec extends ConnectorSpec {

  val nino: String   = "AA123456A"
  val lossId: String = "AAZZ1234567890a"

  class Test extends MockHttpClient with MockAppConfig {

    val connector: BFLossConnector = new BFLossConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.ifsBaseUrl returns baseUrl
    MockAppConfig.ifsToken returns "ifs-token"
    MockAppConfig.ifsEnvironment returns "ifs-environment"
    MockAppConfig.ifsEnvironmentHeaders returns Some(allowedIfsHeaders)
  }

  class DesTest extends MockHttpClient with MockAppConfig {

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

    val bfLoss: CreateBFLossRequestBody = CreateBFLossRequestBody(TypeOfLoss.`self-employment`, "XKIS00000000988", "2019-20", 256.78)

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders)

    val requiredIfsHeadersPost: Seq[(String, String)] = requiredIfsHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(ResponseWrapper(correlationId, CreateBFLossResponse(lossId)))

        MockHttpClient
          .post(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            config = dummyIfsHeaderCarrierConfig,
            body = bfLoss,
            requiredHeaders = requiredIfsHeadersPost,
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
            config = dummyIfsHeaderCarrierConfig,
            body = bfLoss,
            requiredHeaders = requiredIfsHeadersPost,
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
            config = dummyIfsHeaderCarrierConfig,
            body = bfLoss,
            requiredHeaders = requiredIfsHeadersPost,
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

    val amendBFLossResponse: AmendBFLossResponse = AmendBFLossResponse(
      businessId = "XKIS00000000988",
      typeOfLoss = TypeOfLoss.`self-employment`,
      lossAmount = 500.13,
      taxYearBroughtForwardFrom = "2019-20",
      lastModified = "2018-07-13T12:13:48.763Z"
    )

    val amendBFLoss: AmendBFLossRequestBody = AmendBFLossRequestBody(500.13)

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))

    val requiredIfsHeadersPut: Seq[(String, String)] = requiredIfsHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(ResponseWrapper(correlationId, amendBFLossResponse))

        MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyIfsHeaderCarrierConfig,
            body = amendBFLoss,
            requiredHeaders = requiredIfsHeadersPut,
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
            config = dummyIfsHeaderCarrierConfig,
            body = amendBFLoss,
            requiredHeaders = requiredIfsHeadersPut,
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
            config = dummyIfsHeaderCarrierConfig,
            body = amendBFLoss,
            requiredHeaders = requiredIfsHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    def amendBFLossResult(connector: BFLossConnector): DownstreamOutcome[AmendBFLossResponse] =
      await(connector.amendBFLoss(AmendBFLossRequest(nino = Nino(nino), lossId = lossId, amendBFLoss)))
  }

  "delete BFLoss" when {

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new DesTest {
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
      "return an unsuccessful response with the correct correlationId and a single error" in new DesTest {
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
      "return an unsuccessful response with the correct correlationId and multiple errors" in new DesTest {
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
        connector.deleteBFLoss(DeleteBFLossRequest(nino = Nino(nino), lossId = lossId))
      )
  }

  "retrieveBFLoss" should {

    val retrieveResponse: RetrieveBFLossResponse = RetrieveBFLossResponse(
      businessId = "fakeId",
      typeOfLoss = TypeOfLoss.`self-employment`,
      lossAmount = 2000.25,
      taxYearBroughtForwardFrom = "2018-19",
      lastModified = "dateString"
    )

    def retrieveBFLossResult(connector: BFLossConnector): DownstreamOutcome[RetrieveBFLossResponse] =
      await(
        connector.retrieveBFLoss(RetrieveBFLossRequest(nino = Nino(nino), lossId = lossId))
      )

    "return a successful response and correlationId" when {

      "provided with a valid request" in new Test {
        val expected = Right(ResponseWrapper(correlationId, retrieveResponse))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId",
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
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
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
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
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        retrieveBFLossResult(connector) shouldBe expected
      }
    }
  }

  "listBFLosses" should {

    class ListTest extends Test {
      def mockHttpWithParameters(parameters: (String, String)*): CallHandler[Future[DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]]]] =
        MockHttpClient
          .get[DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]]](
            url = s"$baseUrl/income-tax/brought-forward-losses/$nino",
            parameters = parameters,
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )

      def request(taxYear: Option[TaxYear] = None,
                  incomeSourceType: Option[IncomeSourceType] = None,
                  businessId: Option[String] = None): ListBFLossesRequest =
        ListBFLossesRequest(
          nino = Nino(nino),
          taxYearBroughtForwardFrom = taxYear,
          incomeSourceType = incomeSourceType,
          businessId = businessId
        )
    }

    "return a successful response" when {

      // WLOG
      val response =
        ListBFLossesResponse(Seq(ListBFLossesItem("lossId", "businessId", TypeOfLoss.`uk-property-fhl`, 2.75, "2019-20", "lastModified")))

      "provided with no parameters" in new ListTest {
        val expected = Right(ResponseWrapper(correlationId, response))

        mockHttpWithParameters() returns Future.successful(expected)

        await(connector.listBFLosses(request())) shouldBe expected
      }

      "provided with a tax year parameter" in new ListTest {
        val expected = Right(ResponseWrapper(correlationId, response))

        mockHttpWithParameters("taxYear" -> "2019") returns Future.successful(expected)

        await(connector.listBFLosses(request(taxYear = Some(TaxYear("2019"))))) shouldBe expected
      }

      "provided with a income source id parameter" in new ListTest {
        val expected = Right(ResponseWrapper(correlationId, response))

        mockHttpWithParameters("incomeSourceId" -> "testId") returns Future.successful(expected)

        await(connector.listBFLosses(request(businessId = Some("testId")))) shouldBe expected
      }

      "provided with a income source type parameter" in new ListTest {
        val expected = Right(ResponseWrapper(correlationId, response))

        mockHttpWithParameters("incomeSourceType" -> "02") returns Future.successful(expected)

        await(connector.listBFLosses(request(incomeSourceType = Some(IncomeSourceType.`02`)))) shouldBe expected
      }

      "provided with all parameters" in new ListTest {
        val expected = Right(ResponseWrapper(correlationId, response))

        mockHttpWithParameters("taxYear" -> "2019", "incomeSourceId" -> "testId", "incomeSourceType" -> "01") returns Future.successful(expected)

        await(
          connector.listBFLosses(request(taxYear = Some(TaxYear("2019")),
                                         businessId = Some("testId"),
                                         incomeSourceType = Some(IncomeSourceType.`01`)))) shouldBe expected
      }
    }

    "return an unsuccessful response" when {

      "provided with a single error" in new ListTest {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        mockHttpWithParameters() returns Future.successful(expected)

        await(connector.listBFLosses(request())) shouldBe expected
      }

      "provided with multiple errors" in new ListTest {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, LossIdFormatError))))

        mockHttpWithParameters() returns Future.successful(expected)

        await(connector.listBFLosses(request())) shouldBe expected
      }
    }
  }
}
