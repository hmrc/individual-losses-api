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

package v5.lossClaims.create

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.models.domain.Nino
import api.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.HeaderCarrier
import v4.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v5.lossClaims.create.def1.model.request.{Def1_CreateLossClaimRequestBody, Def1_CreateLossClaimRequestData}
import v5.lossClaims.create.def1.model.response.Def1_CreateLossClaimResponse
import v5.lossClaims.create.model.response.CreateLossClaimResponse

import scala.concurrent.Future

class CreateLossClaimConnectorSpec extends ConnectorSpec {

  val nino: String    = "AA123456A"
  val claimId: String = "AAZZ1234567890ag"

  "create LossClaim" when {

    val lossClaim: Def1_CreateLossClaimRequestBody = Def1_CreateLossClaimRequestBody(
      taxYearClaimedFor = "2019-20",
      typeOfLoss = TypeOfLoss.`self-employment`,
      typeOfClaim = TypeOfClaim.`carry-forward`,
      businessId = "XKIS00000000988"
    )

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders)

    val requiredIfsHeadersPost: Seq[(String, String)] = requiredIfsHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new IfsTest with Test {
        val expected: Right[Nothing, ResponseWrapper[CreateLossClaimResponse]] =
          Right(ResponseWrapper(correlationId, Def1_CreateLossClaimResponse(claimId)))

        MockedHttpClient
          .post(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino",
            config = dummyHeaderCarrierConfig,
            body = lossClaim,
            requiredHeaders = requiredIfsHeadersPost,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(expected))

        createLossClaimsResult(connector) shouldBe expected
      }
    }

    def createLossClaimsResult(connector: CreateLossClaimConnector): DownstreamOutcome[CreateLossClaimResponse] =
      await(
        connector.createLossClaim(
          Def1_CreateLossClaimRequestData(
            nino = Nino(nino),
            lossClaim
          )))
  }

  trait Test {
    _: ConnectorTest =>

    val connector: CreateLossClaimConnector = new CreateLossClaimConnector(http = mockHttpClient, appConfig = mockAppConfig)

  }

}
