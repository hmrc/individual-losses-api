/*
 * Copyright 2020 HM Revenue & Customs
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

import mocks.MockAppConfig
import uk.gov.hmrc.domain.Nino
import v1.mocks.MockHttpClient
import v1.models.des._
import v1.models.domain.{AmendBFLoss, BFLoss, TypeOfLoss}
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData._

import scala.concurrent.Future

class BFLossConnectorSpec extends ConnectorSpec {

  lazy val baseUrl = "test-BaseUrl"
  val correlationId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val nino = "AA123456A"
  val lossId = "AAZZ1234567890a"

  class Test extends MockHttpClient with MockAppConfig {
    val connector: BFLossConnector = new BFLossConnector(http = mockHttpClient, appConfig = mockAppConfig)

    val desRequestHeaders: Seq[(String, String)] = Seq("Environment" -> "des-environment", "Authorization" -> s"Bearer des-token")
    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "create BFLoss" when {
    val bfLoss = BFLoss(TypeOfLoss.`self-employment`, Some("XKIS00000000988"), "2019-20", 256.78)
    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, CreateBFLossResponse(lossId)))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/brought-forward-losses/$nino", bfLoss, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        createBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/brought-forward-losses/$nino", bfLoss, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        createBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/brought-forward-losses/$nino", bfLoss, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        createBFLossResult(connector) shouldBe expected
      }
    }

    def createBFLossResult(connector: BFLossConnector): DesOutcome[CreateBFLossResponse] =
      await(
        connector.createBFLoss(
          CreateBFLossRequest(
            nino = Nino(nino),
            bfLoss
          )))
  }

  "amend BFLoss" when {

    val amendBFLossResponse =
      BFLossResponse(selfEmploymentId = Some("XKIS00000000988"),
                          typeOfLoss = TypeOfLoss.`self-employment`,
                          lossAmount = 500.13,
                          taxYear = "2019-20",
                          lastModified = "2018-07-13T12:13:48.763Z")

    val amendBFLoss = AmendBFLoss(500.13)

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, amendBFLossResponse))

        MockedHttpClient
          .put(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", amendBFLoss, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .put(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", amendBFLoss, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockedHttpClient
          .put(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", amendBFLoss, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        amendBFLossResult(connector) shouldBe expected
      }
    }

    def amendBFLossResult(connector: BFLossConnector): DesOutcome[BFLossResponse] =
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
        val expected = Right(DesResponse(correlationId, ()))

        MockedHttpClient
          .delete(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", desRequestHeaders: _*)
          .returns(Future.successful(expected))

        deleteBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .delete(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", desRequestHeaders: _*)
          .returns(Future.successful(expected))

        deleteBFLossResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, LossIdFormatError))))

        MockedHttpClient
          .delete(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", desRequestHeaders: _*)
          .returns(Future.successful(expected))

        deleteBFLossResult(connector) shouldBe expected
      }
    }

    def deleteBFLossResult(connector: BFLossConnector): DesOutcome[Unit] =
      await(
        connector.deleteBFLoss(
          DeleteBFLossRequest(
            nino = Nino(nino),
            lossId = lossId
          )))
  }

  "retrieveBFLoss" should {
    val retrieveResponse = BFLossResponse(Some("fakeId"), TypeOfLoss.`self-employment`, 2000.25, "2018-19", "dateString")

    def retrieveBFLossResult(connector: BFLossConnector): DesOutcome[BFLossResponse] = {
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
        val expected = Left(DesResponse(correlationId, retrieveResponse))

        MockedHttpClient
          .get(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", desRequestHeaders: _*)
          .returns(Future.successful(expected))

        retrieveBFLossResult(connector) shouldBe expected
      }
    }

    "return an unsuccessful response" when {

      "provided with a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .get(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", desRequestHeaders: _*)
          .returns(Future.successful(expected))

        retrieveBFLossResult(connector) shouldBe expected
      }

      "provided with multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, LossIdFormatError))))

        MockedHttpClient
          .get(s"$baseUrl/income-tax/brought-forward-losses/$nino/$lossId", desRequestHeaders: _*)
          .returns(Future.successful(expected))

        retrieveBFLossResult(connector) shouldBe expected
      }
    }
  }

  "listBFLosses" should {
    def listBFLossesResult(connector: BFLossConnector,
                           taxYear: Option[DesTaxYear] = None,
                           incomeSourceType: Option[IncomeSourceType] = None,
                           selfEmploymentId: Option[String] = None): DesOutcome[ListBFLossesResponse[BFLossId]] = {
      await(
        connector.listBFLosses(
          ListBFLossesRequest(
            nino = Nino(nino),
            taxYear = taxYear,
            incomeSourceType = incomeSourceType,
            selfEmploymentId = selfEmploymentId
          )))
    }

    "return a successful response" when {

      "provided with no parameters" in new Test {
        val expected = Left(DesResponse(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/brought-forward-losses/$nino", Seq(), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listBFLossesResult(connector) shouldBe expected
      }

      "provided with a tax year parameter" in new Test {
        val expected = Left(DesResponse(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/brought-forward-losses/$nino", Seq(("taxYear", "2019")), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listBFLossesResult(connector, taxYear = Some(DesTaxYear("2019"))) shouldBe expected
      }

      "provided with a income source id parameter" in new Test {
        val expected = Left(DesResponse(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/brought-forward-losses/$nino", Seq(("incomeSourceId", "testId")), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listBFLossesResult(connector, selfEmploymentId = Some("testId")) shouldBe expected
      }

      "provided with a income source type parameter" in new Test {
        val expected = Left(DesResponse(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/brought-forward-losses/$nino", Seq(("incomeSourceType", "02")), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listBFLossesResult(connector, incomeSourceType = Some(IncomeSourceType.`02`)) shouldBe expected
      }

      "provided with all parameters" in new Test {
        val expected = Left(DesResponse(correlationId, ListBFLossesResponse(Seq(BFLossId("idOne"), BFLossId("idTwo")))))

        MockedHttpClient
          .parameterGet(
            s"$baseUrl/income-tax/brought-forward-losses/$nino",
            Seq(("taxYear", "2019"), ("incomeSourceId", "testId"), ("incomeSourceType", "01")),
            desRequestHeaders: _*
          )
          .returns(Future.successful(expected))

        listBFLossesResult(connector,
                           taxYear = Some(DesTaxYear("2019")),
                           selfEmploymentId = Some("testId"),
                           incomeSourceType = Some(IncomeSourceType.`01`)) shouldBe
          expected
      }
    }

    "return an unsuccessful response" when {

      "provided with a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/brought-forward-losses/$nino", Seq(), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listBFLossesResult(connector) shouldBe expected
      }

      "provided with multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, LossIdFormatError))))

        MockedHttpClient
          .parameterGet(s"$baseUrl/income-tax/brought-forward-losses/$nino", Seq(), desRequestHeaders: _*)
          .returns(Future.successful(expected))

        listBFLossesResult(connector) shouldBe expected
      }
    }
  }
}
