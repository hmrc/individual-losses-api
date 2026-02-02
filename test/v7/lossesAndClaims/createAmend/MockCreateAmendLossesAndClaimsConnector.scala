/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.createAmend

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import shared.connectors.DownstreamOutcome
import uk.gov.hmrc.http.HeaderCarrier
import v7.lossesAndClaims.createAmend.request.CreateAmendLossesAndClaimsRequestData

import scala.concurrent.{ExecutionContext, Future}

trait MockCreateAmendLossesAndClaimsConnector extends TestSuite with MockFactory {

  val connector: CreateAmendLossesAndClaimsConnector = mock[CreateAmendLossesAndClaimsConnector]

  object MockCreateAndAmendLossesAndClaimsConnector {

    def createAndAmendLossesAndClaims(
        createAndAmendLossClaimsRequestData: CreateAmendLossesAndClaimsRequestData): CallHandler[Future[DownstreamOutcome[Unit]]] = {
      (connector
        .amendLossClaimsAndLosses(_: CreateAmendLossesAndClaimsRequestData)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(createAndAmendLossClaimsRequestData, *, *, *)
    }

  }

}
