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

package v6.lossClaims.amendType

import shared.controllers.RequestContext
import shared.services.ServiceOutcome
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import v6.lossClaims.amendType.model.request.AmendLossClaimTypeRequestData
import v6.lossClaims.amendType.model.response.AmendLossClaimTypeResponse

import scala.concurrent.{ExecutionContext, Future}

trait MockAmendLossClaimTypeService extends MockFactory {

  val mockAmendLossClaimTypeService: AmendLossClaimTypeService = mock[AmendLossClaimTypeService]

  object MockAmendLossClaimTypeService {

    def amend(requestData: AmendLossClaimTypeRequestData): CallHandler[Future[ServiceOutcome[AmendLossClaimTypeResponse]]] = {
      (mockAmendLossClaimTypeService
        .amendLossClaimType(_: AmendLossClaimTypeRequestData)(_: RequestContext, _: ExecutionContext))
        .expects(requestData, *, *)
    }

  }

}
