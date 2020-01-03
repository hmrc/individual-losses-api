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

package v1.controllers

import java.util.UUID

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Result
import v1.models.errors.ErrorWrapper

trait BaseController {

  protected val logger = Logger(this.getClass)

  implicit class Response(result: Result) {

    def withApiHeaders(correlationId: String, responseHeaders: (String, String)*): Result = {

      val newHeaders: Seq[(String, String)] = responseHeaders ++ Seq(
        "X-CorrelationId"        -> correlationId,
        "X-Content-Type-Options" -> "nosniff"
      )

      result.copy(header = result.header.copy(headers = result.header.headers ++ newHeaders))
    }
  }

  protected def getCorrelationId(errorWrapper: ErrorWrapper): String = {
    errorWrapper.correlationId match {
      case Some(correlationId) => logger.info(s"[${logger.underlyingLogger}] - " +
        s"Error received from DES ${Json.toJson(errorWrapper)} with correlationId: $correlationId")
        correlationId
      case None =>
        val correlationId = UUID.randomUUID().toString
        logger.info(s"[${getClass.getSimpleName}] -" +
          s"Validation error: ${Json.toJson(errorWrapper)} with correlationId: $correlationId")
        correlationId
    }
  }
}


