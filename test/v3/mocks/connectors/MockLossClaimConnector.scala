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

package v3.mocks.connectors

import api.connectors.DownstreamOutcome
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v3.connectors.LossClaimConnector
import v3.models.request.amendLossClaimType.AmendLossClaimTypeRequest
import v3.models.request.amendLossClaimsOrder.AmendLossClaimsOrderRequest
import v3.models.request.createLossClaim.CreateLossClaimRequest
import v3.models.request.deleteLossClaim.DeleteLossClaimRequest
import v3.models.request.listLossClaims.ListLossClaimsRequest
import v3.models.request.retrieveLossClaim.RetrieveLossClaimRequest
import v3.models.response.amendLossClaimType.AmendLossClaimTypeResponse
import v3.models.response.createLossClaim.CreateLossClaimResponse
import v3.models.response.listLossClaims.{ListLossClaimsItem, ListLossClaimsResponse}
import v3.models.response.retrieveLossClaim.RetrieveLossClaimResponse

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
