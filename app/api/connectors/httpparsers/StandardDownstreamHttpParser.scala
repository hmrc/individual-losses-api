/*
 * Copyright 2022 HM Revenue & Customs
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

package api.connectors.httpparsers

import api.connectors.DownstreamOutcome
import api.models.ResponseWrapper
import api.models.errors.{OutboundError, InternalError}
import play.api.http.Status._
import play.api.libs.json.Reads
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object StandardDownstreamHttpParser extends HttpParser {

  // Return Right[ResponseWrapper[Unit]] as success response has no body - no need to assign it a value
  implicit val readsEmpty: HttpReads[DownstreamOutcome[Unit]] =
    (_: String, url: String, response: HttpResponse) =>
      doRead(NO_CONTENT, url, response) { correlationId =>
        Right(ResponseWrapper(correlationId, ()))
    }

  implicit def reads[A: Reads]: HttpReads[DownstreamOutcome[A]] =
    (_: String, url: String, response: HttpResponse) =>
      doRead(OK, url, response) { correlationId =>
        response.validateJson[A] match {
          case Some(ref) => Right(ResponseWrapper(correlationId, ref))
          case None      => Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
        }
    }

  private def doRead[A](successStatusCode: Int, url: String, response: HttpResponse)(
      successOutcomeFactory: String => DownstreamOutcome[A]): DownstreamOutcome[A] = {

    val correlationId = retrieveCorrelationId(response)

    if (response.status != successStatusCode) {
      logger.warn(
        "[StandardDownstreamHttpParser][read] - " +
          s"MtdError response received from downstream with status: ${response.status} and body\n" +
          s"${response.body} and correlationId: $correlationId when calling $url")
    }

    response.status match {
      case `successStatusCode` =>
        logger.info(
          "[StandardDownstreamHttpParser][read] - " +
            s"Success response received from downstream with correlationId: $correlationId when calling $url")
        successOutcomeFactory(correlationId)

      case BAD_REQUEST | NOT_FOUND | FORBIDDEN | CONFLICT | UNPROCESSABLE_ENTITY => Left(ResponseWrapper(correlationId, parseErrors(response)))
      case INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE                           => Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
      case _                                                                     => Left(ResponseWrapper(correlationId, OutboundError(InternalError)))
    }
  }
}
