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
import api.models.domain.{Nino, Timestamp}
import api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.HeaderCarrier
import v3.models.domain.lossClaim.{ClaimId, TypeOfClaim, TypeOfLoss}
import v3.models.request.amendLossClaimType.{AmendLossClaimTypeRequestBody, AmendLossClaimTypeRequestData}
import v3.models.response.amendLossClaimType.AmendLossClaimTypeResponse

import scala.concurrent.Future

class AmendLossClaimTypeConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  trait Test {
    _: ConnectorTest =>

    val connector: AmendLossClaimTypeConnector = new AmendLossClaimTypeConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

  "amendLossClaimType" when {

    val response: AmendLossClaimTypeResponse = AmendLossClaimTypeResponse(
      businessId = "XKIS00000000988",
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      taxYearClaimedFor = "2019-20",
      sequence = Some(1),
      lastModified = Timestamp("2018-07-13T12:13:48.763Z")
    )

    val amendLossClaimType: AmendLossClaimTypeRequestBody = AmendLossClaimTypeRequestBody(TypeOfClaim.`carry-forward`)

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders)

    val requiredIfsHeadersPut: Seq[(String, String)] = requiredIfsHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        val expected = Right(ResponseWrapper(correlationId, response))

        MockedHttpClient
          .put(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/$claimId",
            config = dummyHeaderCarrierConfig,
            body = amendLossClaimType,
            requiredHeaders = requiredIfsHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        amendLossClaimTypeResult(connector) shouldBe expected
      }
    }

    def amendLossClaimTypeResult(connector: AmendLossClaimTypeConnector): DownstreamOutcome[AmendLossClaimTypeResponse] =
      await(
        connector.amendLossClaimType(
          AmendLossClaimTypeRequestData(
            nino = Nino(nino),
            claimId = ClaimId(claimId),
            amendLossClaimType
          )))
  }

}
