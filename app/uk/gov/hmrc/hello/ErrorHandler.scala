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

package uk.gov.hmrc.hello

import javax.inject.{Inject, Singleton}

import play.api.http.Status._
import play.api.mvc._
import uk.gov.hmrc.hello.controllers._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent
import uk.gov.hmrc.play.bootstrap.http.JsonErrorHandler

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject()(auditConnector: AuditConnector, httpAuditEvent: HttpAuditEvent)
                            (implicit ec: ExecutionContext)
  extends JsonErrorHandler(auditConnector, httpAuditEvent) with ErrorConversion {

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    implicit val req = request

    super.onClientError(request, statusCode, message).map { auditedError =>
      statusCode match {
        case NOT_FOUND => ErrorNotFound
        case BAD_REQUEST => ErrorGenericBadRequest
        case UNAUTHORIZED => ErrorUnauthorized
        case _ => auditedError
      }
    }
  }

  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] = {
    implicit val req = request

    super.onServerError(request, exception).map(_ => ErrorInternalServerError)
  }
}
