/*
 * Copyright 2020 HM Revenue & Customs
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

package v2.mocks.services

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v2.controllers.UserRequest
import v2.models.requestData.ListLossClaimsRequest
import v2.services.{ListLossClaimsOutcome, ListLossClaimsService}

import scala.concurrent.{ExecutionContext, Future}

trait MockListLossClaimsService extends MockFactory {

  val mockListLossClaimsService: ListLossClaimsService = mock[ListLossClaimsService]

  object MockListLossClaimsService {

    def list[A](listBFLossRequest: ListLossClaimsRequest): CallHandler[Future[ListLossClaimsOutcome]] = {
      (mockListLossClaimsService
        .listLossClaims(_: ListLossClaimsRequest)(_: HeaderCarrier, _: ExecutionContext, _:UserRequest[A]))
        .expects(listBFLossRequest, *, *, *)
    }
  }
}
