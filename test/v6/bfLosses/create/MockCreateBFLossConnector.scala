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

package v6.bfLosses.create

import shared.connectors.DownstreamOutcome
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.http.HeaderCarrier
import v6.bfLosses.create.CreateBFLossConnector
import v6.bfLosses.create.model.request.CreateBFLossRequestData
import v6.bfLosses.create.model.response.CreateBFLossResponse

import scala.concurrent.{ExecutionContext, Future}

trait MockCreateBFLossConnector extends TestSuite with MockFactory {

  val connector: CreateBFLossConnector = mock[CreateBFLossConnector]

  object MockCreateBFLossConnector {

    def createBFLoss(createBFLossRequest: CreateBFLossRequestData): CallHandler[Future[DownstreamOutcome[CreateBFLossResponse]]] = {
      (connector
        .createBFLoss(_: CreateBFLossRequestData)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(createBFLossRequest, *, *, *)
    }

  }

}
