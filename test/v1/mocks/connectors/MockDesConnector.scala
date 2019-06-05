/*
 * Copyright 2019 HM Revenue & Customs
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

package v1.mocks.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import v1.connectors.{DesConnector, DesOutcome, DesUri}

import scala.concurrent.{ExecutionContext, Future}

trait MockDesConnector extends MockFactory {
  val connector: DesConnector = mock[DesConnector]

  object MockedDesConnector {

    def post[Body, Resp](body: Body, uri: DesUri[Resp]): CallHandler[Future[DesOutcome[Resp]]] = {
      (connector
        .post(_: Body, _: DesUri[Resp])(_: Writes[Body], _: ExecutionContext, _: HeaderCarrier, _: HttpReads[DesOutcome[Resp]]))
        .expects(body, uri, *, *, *, *)
    }

    def get[Resp](uri: DesUri[Resp]): CallHandler[Future[DesOutcome[Resp]]] = {
      (connector
        .get(_: DesUri[Resp])(_: ExecutionContext, _: HeaderCarrier, _: HttpReads[DesOutcome[Resp]]))
        .expects(uri, *, *, *)
    }
  }

}
