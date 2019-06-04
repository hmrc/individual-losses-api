/*
 * Copyright 2019 HM Revenue & Customs
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

package v2.orchestrators

import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v2.controllers.EndpointLogContext
import v2.mocks.services.MockSampleService
import v2.models.des.DesSampleResponse
import v2.models.domain.{SampleRequestBody, SampleResponse}
import v2.models.errors._
import v2.models.outcomes.ResponseWrapper
import v2.models.requestData.{DesTaxYear, SampleRequestData}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SampleOrchestratorSpec extends UnitSpec with MockSampleService {

  private val nino = "AA123456A"
  private val taxYear = "2017-18"
  private val correlationId = "X-123"

  private val requestBody = SampleRequestBody("someData")

  private val requestData = SampleRequestData(Nino(nino), DesTaxYear.fromMtd(taxYear), requestBody)

  trait Test {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val orchestrator = new SampleOrchestrator(
      sampleService = mockSampleService
    )
  }

  "orchestrator" when {
    "service call successsful" must {
      "return mapped result" in new Test {
        MockSampleService.doService(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, DesSampleResponse("result")))))

        await(orchestrator.orchestrate(requestData)) shouldBe Right(ResponseWrapper(correlationId, SampleResponse("result")))
      }
    }

    "unsuccessful" must {
      "map errors according to spec" when {

        def serviceError(desErrorCode: String, error: MtdError): Unit =
          s"a $desErrorCode error is returned from the service" in new Test {

            MockSampleService.doService(requestData)
              .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

            await(orchestrator.orchestrate(requestData)) shouldBe Left(ErrorWrapper(Some(correlationId), error))
          }

        val input = Seq(
          ("INVALID_NINO", NinoFormatError),
          ("INVALID_TAX_YEAR", TaxYearFormatError),
          ("SERVER_ERROR", DownstreamError),
          ("SERVICE_UNAVAILABLE", DownstreamError)
        )

        input.foreach(args => (serviceError _).tupled(args))
      }
    }
  }
}
