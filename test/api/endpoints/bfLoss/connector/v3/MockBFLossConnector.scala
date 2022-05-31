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

package api.endpoints.bfLoss.connector.v3

import api.connectors.DownstreamOutcome
import api.endpoints.bfLoss.amend.v3.request.AmendBFLossRequest
import api.endpoints.bfLoss.amend.v3.response.AmendBFLossResponse
import api.endpoints.bfLoss.create.v3.request.CreateBFLossRequest
import api.endpoints.bfLoss.create.v3.response.CreateBFLossResponse
import api.endpoints.bfLoss.delete.v3.request.DeleteBFLossRequest
import api.endpoints.bfLoss.list.v3.request.ListBFLossesRequest
import api.endpoints.bfLoss.list.v3.response.{ListBFLossesItem, ListBFLossesResponse}
import api.endpoints.bfLoss.retrieve.v3.request.RetrieveBFLossRequest
import api.endpoints.bfLoss.retrieve.v3.response.RetrieveBFLossResponse
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockBFLossConnector extends MockFactory {
  val connector: BFLossConnector = mock[BFLossConnector]

  object MockedBFLossConnector {

    def createBFLoss(createBFLossRequest: CreateBFLossRequest): CallHandler[Future[DownstreamOutcome[CreateBFLossResponse]]] = {
      (connector
        .createBFLoss(_: CreateBFLossRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(createBFLossRequest, *, *)
    }

    def amendBFLoss(amendBFLossRequest: AmendBFLossRequest): CallHandler[Future[DownstreamOutcome[AmendBFLossResponse]]] = {
      (connector
        .amendBFLoss(_: AmendBFLossRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(amendBFLossRequest, *, *)
    }

    def deleteBFLoss(deleteBFLossRequest: DeleteBFLossRequest): CallHandler[Future[DownstreamOutcome[Unit]]] = {
      (connector
        .deleteBFLoss(_: DeleteBFLossRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(deleteBFLossRequest, *, *)
    }

    def retrieveBFLoss(request: RetrieveBFLossRequest): CallHandler[Future[DownstreamOutcome[RetrieveBFLossResponse]]] = {
      (connector
        .retrieveBFLoss(_: RetrieveBFLossRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }

    def listBFLosses(request: ListBFLossesRequest): CallHandler[Future[DownstreamOutcome[ListBFLossesResponse[ListBFLossesItem]]]] = {
      (connector
        .listBFLosses(_: ListBFLossesRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }
  }

}