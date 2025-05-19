/*
 * Copyright 2025 HM Revenue & Customs
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

package v5.lossClaims.retrieve

import shared.connectors.DownstreamOutcome
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier
import v5.lossClaims.retrieve.model.request.RetrieveLossClaimRequestData
import v5.lossClaims.retrieve.model.response.RetrieveLossClaimResponse

import scala.concurrent.{ExecutionContext, Future}

trait MockRetrieveLossClaimConnector extends TestSuite with MockFactory {
  val mockRetrieveLossClaimConnector: RetrieveLossClaimConnector = mock[RetrieveLossClaimConnector]

  object MockRetrieveLossClaimConnector {

    def retrieveLossClaim(request: RetrieveLossClaimRequestData,
                          isAmendRequest: Boolean): CallHandler[Future[DownstreamOutcome[RetrieveLossClaimResponse]]] =
      (mockRetrieveLossClaimConnector
        .retrieveLossClaim(_: RetrieveLossClaimRequestData, _: Boolean)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(request, isAmendRequest, *, *, *)

  }

}
