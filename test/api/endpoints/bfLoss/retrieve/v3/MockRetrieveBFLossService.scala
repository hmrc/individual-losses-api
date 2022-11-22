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

package api.endpoints.bfLoss.retrieve.v3

import api.endpoints.bfLoss.retrieve.v3.request.RetrieveBFLossRequest
import api.services.v3.Outcomes.RetrieveBFLossOutcome
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockRetrieveBFLossService extends MockFactory {

  val mockRetrieveBFLossService: RetrieveBFLossService = mock[RetrieveBFLossService]

  object MockRetrieveBFLossService {

    def retrieve(retrieveBFLossRequest: RetrieveBFLossRequest): CallHandler[Future[RetrieveBFLossOutcome]] = {
      (mockRetrieveBFLossService
        .retrieveBFLoss(_: RetrieveBFLossRequest)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(retrieveBFLossRequest, *, *, *)
    }
  }
}
