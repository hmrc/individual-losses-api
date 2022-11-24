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

package api.endpoints.lossClaim.list.v3.connector

import api.connectors.{ ConnectorSpec, DownstreamOutcome }
import api.endpoints.lossClaim.connector.v3.LossClaimConnector
import api.endpoints.lossClaim.domain.v3.{ TypeOfClaim, TypeOfLoss }
import api.endpoints.lossClaim.list.v3.request.ListLossClaimsRequest
import api.endpoints.lossClaim.list.v3.response.{ ListLossClaimsItem, ListLossClaimsResponse }
import api.models.ResponseWrapper
import api.models.domain.{ Nino, TaxYear }
import api.models.errors._

import scala.concurrent.Future

class ListLossClaimsConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  trait Test {
    _: ConnectorTest =>

    val connector: LossClaimConnector = new LossClaimConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "list LossClaims" when {
    "a valid request is supplied with no query parameters" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        val expected = Right(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(Seq(
              ListLossClaimsItem("businessId",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId",
                                 Some(1),
                                 "2020-07-13T12:13:48.763Z"),
              ListLossClaimsItem("businessId1",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId1",
                                 Some(2),
                                 "2020-07-13T12:13:48.763Z")
            ))
          ))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(),
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector) shouldBe expected
      }
    }

    "provided with a tax year parameter" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(Seq(
              ListLossClaimsItem("businessId",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId",
                                 Some(1),
                                 "2020-07-13T12:13:48.763Z"),
              ListLossClaimsItem("businessId1",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId1",
                                 Some(2),
                                 "2020-07-13T12:13:48.763Z")
            ))
          ))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("taxYear", "2019")),
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, taxYear = Some(TaxYear("2019"))) shouldBe expected
      }
    }

    "provided with a income source id parameter" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(Seq(
              ListLossClaimsItem("businessId",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId",
                                 Some(1),
                                 "2020-07-13T12:13:48.763Z"),
              ListLossClaimsItem("businessId1",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId1",
                                 Some(2),
                                 "2020-07-13T12:13:48.763Z")
            ))
          ))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("incomeSourceId", "testId")),
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, businessId = Some("testId")) shouldBe expected
      }
    }

    "provided with a income source type parameter" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(Seq(
              ListLossClaimsItem("businessId",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId",
                                 Some(1),
                                 "2020-07-13T12:13:48.763Z"),
              ListLossClaimsItem("businessId1",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId1",
                                 Some(2),
                                 "2020-07-13T12:13:48.763Z")
            ))
          ))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("incomeSourceType", "02")),
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, typeOfLoss = Some(TypeOfLoss.`uk-property-non-fhl`)) shouldBe expected
      }
    }

    "provided with a claim type parameter" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(Seq(
              ListLossClaimsItem("businessId",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId",
                                 Some(1),
                                 "2020-07-13T12:13:48.763Z"),
              ListLossClaimsItem("businessId1",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId1",
                                 Some(2),
                                 "2020-07-13T12:13:48.763Z")
            ))
          ))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("claimType", "CSGI")),
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, claimType = Some(TypeOfClaim.`carry-sideways`)) shouldBe expected
      }
    }

    "provided with all parameters" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        val expected = Left(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(Seq(
              ListLossClaimsItem("businessId",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId",
                                 Some(1),
                                 "2020-07-13T12:13:48.763Z"),
              ListLossClaimsItem("businessId1",
                                 TypeOfClaim.`carry-sideways`,
                                 TypeOfLoss.`self-employment`,
                                 "2020",
                                 "claimId1",
                                 Some(2),
                                 "2020-07-13T12:13:48.763Z")
            ))
          ))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("taxYear", "2019"), ("incomeSourceId", "testId"), ("incomeSourceType", "01"), ("claimType", "CSGI")),
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(
          connector = connector,
          taxYear = Some(TaxYear("2019")),
          businessId = Some("testId"),
          typeOfLoss = Some(TypeOfLoss.`self-employment`),
          claimType = Some(TypeOfClaim.`carry-sideways`)
        ) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new IfsTest with Test {
        val expected = Left(ResponseWrapper(correlationId, SingleError(NinoFormatError)))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(),
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new IfsTest with Test {
        val expected = Left(ResponseWrapper(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockHttpClient
          .get(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            parameters = Seq(("taxYear", "2019")),
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        listLossClaimsResult(connector = connector, Some(TaxYear("2019"))) shouldBe expected
      }
    }

    def listLossClaimsResult(connector: LossClaimConnector,
                             taxYear: Option[TaxYear] = None,
                             typeOfLoss: Option[TypeOfLoss] = None,
                             businessId: Option[String] = None,
                             claimType: Option[TypeOfClaim] = None): DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]] =
      await(
        connector.listLossClaims(
          ListLossClaimsRequest(
            nino = Nino(nino),
            taxYearClaimedFor = taxYear,
            typeOfLoss = typeOfLoss,
            businessId = businessId,
            typeOfClaim = claimType
          )))
  }
}
