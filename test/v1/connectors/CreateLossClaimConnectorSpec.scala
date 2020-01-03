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
import v1.models.des._
import v1.models.domain._
import v1.models.errors._
import v1.models.outcomes.DesResponse
import v1.models.requestData._

import scala.concurrent.Future

class CreateLossClaimConnectorSpec extends LossClaimConnectorSpec {

  "create LossClaim" when {
    val lossClaim = LossClaim("2019-20", TypeOfLoss.`self-employment`, TypeOfClaim.`carry-forward`, Some("XKIS00000000988"))
    "a valid request is supplied" should {
      "return a successful response with the correct correlationId" in new Test {
        val expected = Right(DesResponse(correlationId, CreateLossClaimResponse(claimId)))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/claims-for-relief/$nino", lossClaim, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        createLossClaimsResult(connector) shouldBe expected
      }
    }

    "a request returning a single error" should {
      "return an unsuccessful response with the correct correlationId and a single error" in new Test {
        val expected = Left(DesResponse(correlationId, SingleError(NinoFormatError)))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/claims-for-relief/$nino", lossClaim, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        createLossClaimsResult(connector) shouldBe expected
      }
    }

    "a request returning multiple errors" should {
      "return an unsuccessful response with the correct correlationId and multiple errors" in new Test {
        val expected = Left(DesResponse(correlationId, MultipleErrors(Seq(NinoFormatError, TaxYearFormatError))))

        MockedHttpClient
          .post(s"$baseUrl/income-tax/claims-for-relief/$nino", lossClaim, desRequestHeaders: _*)
          .returns(Future.successful(expected))

        createLossClaimsResult(connector) shouldBe expected
      }
    }

    def createLossClaimsResult(connector: LossClaimConnector): DesOutcome[CreateLossClaimResponse] =
      await(
        connector.createLossClaim(
          CreateLossClaimRequest(
            nino = Nino(nino),
            lossClaim
          )))
  }
}
