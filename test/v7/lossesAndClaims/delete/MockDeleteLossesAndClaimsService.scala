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

package v7.lossesAndClaims.delete

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import shared.controllers.RequestContext
import shared.services.ServiceOutcome
import v7.lossesAndClaims.delete.model.request.DeleteLossesAndClaimsRequestData

import scala.concurrent.{ExecutionContext, Future}

trait MockDeleteLossesAndClaimsService extends TestSuite with MockFactory {

  val mockDeleteLossClaimsService: DeleteLossesAndClaimsService = mock[DeleteLossesAndClaimsService]

  object MockDeleteLossesAndClaimsService {

    def delete(requestData: DeleteLossesAndClaimsRequestData): CallHandler[Future[ServiceOutcome[Unit]]] = {
      (mockDeleteLossClaimsService
        .deleteLossClaimsService(_: DeleteLossesAndClaimsRequestData)(_: RequestContext, _: ExecutionContext))
        .expects(requestData, *, *)
    }

  }

}
