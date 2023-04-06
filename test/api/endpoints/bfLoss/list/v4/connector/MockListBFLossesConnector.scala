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

package api.endpoints.bfLoss.list.v4.connector

import api.connectors.DownstreamOutcome
import api.endpoints.bfLoss.list.v4.request.ListBFLossesRequest
import api.endpoints.bfLoss.list.v4.response.{ ListBFLossesItem, ListBFLossesResponse }
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ ExecutionContext, Future }

trait MockListBFLossesConnector extends MockFactory {
  val connector: ListBFLossesConnector = mock[ListBFLossesConnector]

  object MockedListBFLossesConnector {

    def listBFLosses(request: ListBFLossesRequest): CallHandler[Future[DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]]]] = {
      (connector
        .listBFLosses(_: ListBFLossesRequest)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(request, *, *, *)
    }

  }

}
