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

package v3.connectors

import uk.gov.hmrc.http.HeaderCarrier
import v3.models.domain.{AmendLossClaimsOrderRequestBody, Claim, Nino, TypeOfClaim}
import v3.models.outcomes.ResponseWrapper
import v3.models.requestData.{AmendLossClaimsOrderRequest, DownstreamTaxYear}

import scala.concurrent.Future

class AmendLossClaimsOrderConnectorSpec extends LossClaimConnectorSpec {

  val taxYear: String = "2019-20"

  "amendLossClaimsOrderV3" when {

    val amendLossClaimsOrder: AmendLossClaimsOrderRequestBody = AmendLossClaimsOrderRequestBody(
      claimType = TypeOfClaim.`carry-sideways`,
      listOfLossClaims = Seq(
        Claim("1234568790ABCDE", 1),
        Claim("1234568790ABCDF", 2)
      )
    )

    implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders)

    val requiredDesHeadersPut: Seq[(String, String)] = requiredDesHeaders ++ Seq("Content-Type" -> "application/json")

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new DesTest {
        val expected = Right(ResponseWrapper(correlationId, ()))

          MockHttpClient
          .put(
            url = s"$baseUrl/income-tax/claims-for-relief/$nino/preferences/${DownstreamTaxYear.fromMtd(taxYear)}",
            config = dummyDesHeaderCarrierConfig,
            body = amendLossClaimsOrder,
            requiredHeaders = requiredDesHeadersPut,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          ).returns(Future.successful(expected))

        amendLossClaimsOrderResult(connector) shouldBe expected
      }
    }

    def amendLossClaimsOrderResult(connector: LossClaimConnector): DownstreamOutcome[Unit] =
      await(
        connector.amendLossClaimsOrder(
          AmendLossClaimsOrderRequest(
            nino = Nino(nino),
            taxYear = DownstreamTaxYear.fromMtd(taxYear),
            body = amendLossClaimsOrder
          )))
  }
}