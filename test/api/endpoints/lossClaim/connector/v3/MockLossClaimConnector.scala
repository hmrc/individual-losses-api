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

package api.endpoints.lossClaim.connector.v3

import api.connectors.DownstreamOutcome
import api.endpoints.lossClaim.amendOrder.v3.request.AmendLossClaimsOrderRequest
import api.endpoints.lossClaim.amendType.v3.request.AmendLossClaimTypeRequest
import api.endpoints.lossClaim.amendType.v3.response.AmendLossClaimTypeResponse
import api.endpoints.lossClaim.create.v3.request.CreateLossClaimRequest
import api.endpoints.lossClaim.create.v3.response.CreateLossClaimResponse
import api.endpoints.lossClaim.delete.v3.request.DeleteLossClaimRequest
import api.endpoints.lossClaim.list.v3.request.ListLossClaimsRequest
import api.endpoints.lossClaim.list.v3.response.{ListLossClaimsItem, ListLossClaimsResponse}
import api.endpoints.lossClaim.retrieve.v3.request.RetrieveLossClaimRequest
import api.endpoints.lossClaim.retrieve.v3.response.RetrieveLossClaimResponse
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockLossClaimConnector extends MockFactory {
  val connector: LossClaimConnector = mock[LossClaimConnector]

  object MockedLossClaimConnector {

    def createLossClaim(request: CreateLossClaimRequest): CallHandler[Future[DownstreamOutcome[CreateLossClaimResponse]]] = {
      (connector
        .createLossClaim(_: CreateLossClaimRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }

    def amendLossClaimType(
        amendLossClaimTypeRequest: AmendLossClaimTypeRequest): CallHandler[Future[DownstreamOutcome[AmendLossClaimTypeResponse]]] = {
      (connector
        .amendLossClaimType(_: AmendLossClaimTypeRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(amendLossClaimTypeRequest, *, *)
    }

    def retrieveLossClaim(request: RetrieveLossClaimRequest): CallHandler[Future[DownstreamOutcome[RetrieveLossClaimResponse]]] = {
      (connector
        .retrieveLossClaim(_: RetrieveLossClaimRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }

    def deleteLossClaim(deleteLossClaimRequest: DeleteLossClaimRequest): CallHandler[Future[DownstreamOutcome[Unit]]] = {
      (connector
        .deleteLossClaim(_: DeleteLossClaimRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(deleteLossClaimRequest, *, *)
    }

    def listLossClaims(request: ListLossClaimsRequest): CallHandler[Future[DownstreamOutcome[ListLossClaimsResponse[ListLossClaimsItem]]]] = {
      (connector
        .listLossClaims(_: ListLossClaimsRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }

    def amendLossClaimsOrder(request: AmendLossClaimsOrderRequest): CallHandler[Future[DownstreamOutcome[Unit]]] = {
      (connector
        .amendLossClaimsOrder(_: AmendLossClaimsOrderRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }
  }
}
