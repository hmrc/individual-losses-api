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

import uk.gov.hmrc.domain.Nino
import v1.models.des.ReliefClaimed
import v1.models.domain.{Claim, LossClaimsList}
import v1.models.outcomes.DesResponse
import v1.models.requestData.{AmendLossClaimsOrderRequest, DesTaxYear}

import scala.concurrent.Future

class AmendLossClaimsOrderConnectorSpec extends LossClaimConnectorSpec {

  val taxYear = "2019-20"

  "amendLossClaimsOrder" when {

    val amendLossClaimsOrder = LossClaimsList(ReliefClaimed.`CSGI`,
      Seq(Claim("1234568790ABCDE", 1),
          Claim("1234568790ABCDF", 2)))

    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {

        val expected = Right(DesResponse(correlationId, ()))

          MockedHttpClient
          .put(s"$baseUrl/income-tax/claims-for-relief/$nino/preferences/${DesTaxYear.fromMtd(taxYear)}", amendLossClaimsOrder, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        amendLossClaimsOrderResult(connector) shouldBe expected
      }
    }

    def amendLossClaimsOrderResult(connector: LossClaimConnector): DesOutcome[Unit] =
      await(
        connector.amendLossClaimsOrder(
          AmendLossClaimsOrderRequest(
            nino = Nino(nino),
            taxYear = DesTaxYear.fromMtd(taxYear),
            body = amendLossClaimsOrder
          )))
  }
}
