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

package v7.lossClaims.delete

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import shared.connectors.DownstreamOutcome
import uk.gov.hmrc.http.HeaderCarrier
import v7.lossClaims.delete.model.request.DeleteLossClaimsRequestData

import scala.concurrent.{ExecutionContext, Future}

trait MockDeleteLossClaimsConnector extends TestSuite with MockFactory {

  val connector: DeleteLossClaimsConnector = mock[DeleteLossClaimsConnector]

  object MockDeleteLossClaimsConnector {

    def deleteLossClaims(deleteLossClaimsRequestData: DeleteLossClaimsRequestData): CallHandler[Future[DownstreamOutcome[Unit]]] = {
      (connector
        .deleteLossClaims(_: DeleteLossClaimsRequestData)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(deleteLossClaimsRequestData, *, *, *)
    }

  }

}
