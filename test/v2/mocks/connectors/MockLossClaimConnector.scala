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

package v2.mocks.connectors

import api.connectors.DownstreamOutcome
import api.connectors.v2.LossClaimConnector
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v2.models.des.{CreateLossClaimResponse, ListLossClaimsResponse, LossClaimId, LossClaimResponse}
import v2.models.requestData._

import scala.concurrent.{ExecutionContext, Future}

trait MockLossClaimConnector extends MockFactory {
  val connector: LossClaimConnector = mock[LossClaimConnector]

  object MockedLossClaimConnector {

    def createLossClaim(request: CreateLossClaimRequest): CallHandler[Future[DownstreamOutcome[CreateLossClaimResponse]]] = {
      (connector
        .createLossClaim(_: CreateLossClaimRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }

    def amendLossClaim(amendLossClaimRequest: AmendLossClaimRequest): CallHandler[Future[DownstreamOutcome[LossClaimResponse]]] = {
      (connector
        .amendLossClaim(_: AmendLossClaimRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(amendLossClaimRequest, *, *)
    }

    def retrieveLossClaim(request: RetrieveLossClaimRequest): CallHandler[Future[DownstreamOutcome[LossClaimResponse]]] = {
      (connector
        .retrieveLossClaim(_: RetrieveLossClaimRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(request, *, *)
    }

    def deleteLossClaim(deleteLossClaimRequest: DeleteLossClaimRequest): CallHandler[Future[DownstreamOutcome[Unit]]] = {
      (connector
        .deleteLossClaim(_: DeleteLossClaimRequest)(_: HeaderCarrier, _: ExecutionContext))
        .expects(deleteLossClaimRequest, *, *)
    }

    def listLossClaims(request: ListLossClaimsRequest): CallHandler[Future[DownstreamOutcome[ListLossClaimsResponse[LossClaimId]]]] = {
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
