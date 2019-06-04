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

import cats.data.EitherT
import cats.implicits._
import javax.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import v2.controllers.EndpointLogContext
import v2.models.domain.SampleResponse
import v2.models.errors.{DownstreamError, ErrorWrapper, NinoFormatError, TaxYearFormatError}
import v2.models.outcomes.ResponseWrapper
import v2.models.requestData.SampleRequestData
import v2.services.SampleService

import scala.concurrent.{ExecutionContext, Future}

class SampleOrchestrator @Inject()(sampleService: SampleService) extends DesResponseMappingSupport {

  def orchestrate(request: SampleRequestData)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    logContext: EndpointLogContext): Future[Either[ErrorWrapper, ResponseWrapper[SampleResponse]]] = {

    val result = for {
      desResponse <- EitherT(sampleService.doService(request)).leftMap(mapDesErrors(desErrorMap))
    } yield desResponse.map(des => SampleResponse(des.responseData))

    result.value
  }

  private def desErrorMap =
    Map(
      "INVALID_NINO" -> NinoFormatError,
      "INVALID_TAX_YEAR" -> TaxYearFormatError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    )
}
