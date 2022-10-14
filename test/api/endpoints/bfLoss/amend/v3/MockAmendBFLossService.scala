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

package api.endpoints.bfLoss.amend.v3

import api.endpoints.bfLoss.amend.v3.request.AmendBFLossRequest
import api.services.v3.Outcomes.AmendBFLossOutcome
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAmendBFLossService extends MockFactory {

  val mockAmendBFLossService: AmendBFLossService = mock[AmendBFLossService]

  object MockAmendBFLossService {

    def amend(requestData: AmendBFLossRequest): CallHandler[Future[AmendBFLossOutcome]] = {
      (mockAmendBFLossService
        .amendBFLoss(_: AmendBFLossRequest)(_: HeaderCarrier,
          _: ExecutionContext,
          _: String))
        .expects(requestData, *, *, *)
    }
  }
}
