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

package v5.lossClaims.amendOrder

import api.connectors.DownstreamOutcome
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v5.lossClaims.amendOrder.AmendLossClaimsOrderConnector
import v5.lossClaims.amendOrder.model.request.AmendLossClaimsOrderRequestData

import scala.concurrent.{ExecutionContext, Future}

trait MockAmendLossClaimsConnector extends MockFactory {

  val mockAmendLossClaimsConnector: AmendLossClaimsOrderConnector = mock[AmendLossClaimsOrderConnector]

  object MockAmendLossClaimsConnector {

    def amendLossClaimsOrder(request: AmendLossClaimsOrderRequestData): CallHandler[Future[DownstreamOutcome[Unit]]] = {
      (mockAmendLossClaimsConnector
        .amendLossClaimsOrder(_: AmendLossClaimsOrderRequestData)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(request, *, *, *)
    }

  }

}
