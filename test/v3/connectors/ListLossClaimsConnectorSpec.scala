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

package v3.connectors

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import v3.fixtures.ListLossClaimsFixtures._
import api.models.ResponseWrapper
import api.models.domain.{Nino, TaxYear}
import api.models.errors.{DownstreamErrorCode, DownstreamErrors, InternalError, OutboundError}
import v3.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v3.models.request.listLossClaims.ListLossClaimsRequest
import v3.models.response.listLossClaims.{ListLossClaimsItem, ListLossClaimsResponse}

import scala.concurrent.Future

class ListLossClaimsConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  trait Test {
    _: ConnectorTest =>
    val connector: ListLossClaimsConnector = new ListLossClaimsConnector(http = mockHttpClient, appConfig = mockAppConfig)

    protected def success(taxYear: String) = Right(ResponseWrapper(correlationId, singleClaimResponseModel(taxYear)))

    protected def downstreamError(code: String) = Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(code))))

    val outboundError = Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
  }

  "list LossClaims" when {
    "a valid request is supplied with no tax year parameter" should {
      "return a successful combined response" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/19-20/claims-for-relief/$nino") returns Future.successful(success("2019-20"))
        willGet(s"$baseUrl/income-tax/20-21/claims-for-relief/$nino") returns Future.successful(success("2020-21"))
        willGet(s"$baseUrl/income-tax/21-22/claims-for-relief/$nino") returns Future.successful(success("2021-22"))
        willGet(s"$baseUrl/income-tax/22-23/claims-for-relief/$nino") returns Future.successful(success("2022-23"))

        listLossClaimsResult(connector) shouldBe Right(ResponseWrapper(correlationId, multipleClaimsResponseModel))
      }

      "return a successful response ignoring 404 NOT_FOUND responses" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/19-20/claims-for-relief/$nino") returns Future.successful(success("2019-20"))
        willGet(s"$baseUrl/income-tax/20-21/claims-for-relief/$nino") returns Future.successful(success("2020-21"))
        willGet(s"$baseUrl/income-tax/21-22/claims-for-relief/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/22-23/claims-for-relief/$nino") returns Future.successful(success("2022-23"))

        listLossClaimsResult(connector) shouldBe Right(
          ResponseWrapper(
            correlationId,
            ListLossClaimsResponse(
              List(
                listLossClaim("2019-20"),
                listLossClaim("2020-21"),
                listLossClaim("2022-23")
              )
            )
          )
        )
      }

      "return a 404 NOT_FOUND response if all responses are NOT_FOUND" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/19-20/claims-for-relief/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/20-21/claims-for-relief/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/21-22/claims-for-relief/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/22-23/claims-for-relief/$nino") returns Future.successful(downstreamError("NOT_FOUND"))

        listLossClaimsResult(connector) shouldBe Left(
          ResponseWrapper(
            correlationId,
            DownstreamErrors.single(DownstreamErrorCode("NOT_FOUND"))
          )
        )
      }

      "return the error response if any request errors" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/19-20/claims-for-relief/$nino") returns Future.successful(success("2019-20"))
        willGet(s"$baseUrl/income-tax/20-21/claims-for-relief/$nino") returns Future.successful(success("2020-21"))
        willGet(s"$baseUrl/income-tax/21-22/claims-for-relief/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/22-23/claims-for-relief/$nino") returns Future.successful(downstreamError("INVALID_TAXABLE_ENTITY_ID"))

        listLossClaimsResult(connector) shouldBe Left(
          ResponseWrapper(
            correlationId,
            DownstreamErrors.single(DownstreamErrorCode("INVALID_TAXABLE_ENTITY_ID"))
          )
        )
      }

      "return the error response for an OutboundError" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/19-20/claims-for-relief/$nino") returns Future.successful(success("2019-20"))
        willGet(s"$baseUrl/income-tax/20-21/claims-for-relief/$nino") returns Future.successful(success("2020-21"))
        willGet(s"$baseUrl/income-tax/21-22/claims-for-relief/$nino") returns Future.successful(downstreamError("NOT_FOUND"))
        willGet(s"$baseUrl/income-tax/22-23/claims-for-relief/$nino") returns Future.successful(outboundError)

        listLossClaimsResult(connector) shouldBe Left(
          ResponseWrapper(correlationId, OutboundError(InternalError))
        )
      }
    }

    "a valid request is supplied with only the tax year parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(s"$baseUrl/income-tax/23-24/claims-for-relief/$nino") returns Future.successful(success("2023-24"))
        listLossClaimsResult(connector, Some(TaxYear.fromMtd("2023-24"))) shouldBe success("2023-24")
      }
    }

    "provided with a income source id parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino", queryParams = List(("incomeSourceId", "testId"))) returns Future
          .successful(success("2023-24"))

        listLossClaimsResult(connector = connector, Some(TaxYear.fromMtd("2023-24")), businessId = Some("testId")) shouldBe success("2023-24")
      }
    }

    "provided with a income source type parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          queryParams = List(("incomeSourceType", "02"))
        ) returns Future.successful(success("2023-24"))

        listLossClaimsResult(
          connector = connector,
          Some(TaxYear.fromMtd("2023-24")),
          typeOfLoss = Some(TypeOfLoss.`uk-property-non-fhl`)) shouldBe success("2023-24")
      }
    }

    "provided with a claim type parameter" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          queryParams = List(("claimType", "CSGI"))
        ) returns Future.successful(success("2023-24"))

        listLossClaimsResult(
          connector = connector,
          Some(TaxYear.fromMtd("2023-24")),
          claimType = Some(TypeOfClaim.`carry-sideways`)) shouldBe success("2023-24")
      }
    }

    "provided with all parameters" should {
      "return a successful response with the correct correlationId" in new TysIfsTest with Test {
        willGet(
          url = s"$baseUrl/income-tax/23-24/claims-for-relief/$nino",
          queryParams = List(("incomeSourceId", "testId"), ("incomeSourceType", "01"), ("claimType", "CSGI"))
        ) returns Future.successful(success("2023-24"))

        listLossClaimsResult(
          connector = connector,
          taxYear = Some(TaxYear("2024")),
          businessId = Some("testId"),
          typeOfLoss = Some(TypeOfLoss.`self-employment`),
          claimType = Some(TypeOfClaim.`carry-sideways`)
        ) shouldBe success("2023-24")
      }
    }

    def listLossClaimsResult(connector: ListLossClaimsConnector,
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
