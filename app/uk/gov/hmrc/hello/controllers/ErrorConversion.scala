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

package uk.gov.hmrc.hello.controllers

import play.api.http.HeaderNames.ACCEPT
import play.api.http.MimeTypes
import play.api.libs.json.Json
import play.api.mvc.{RequestHeader, Result, Results}

trait ErrorConversion {

  import HmrcMimeTypes._
  import Results._
  import XmlHeaderHandling._

  implicit def toResult[T](error: ErrorResponse)(implicit request: RequestHeader): Result = toResult(error, request.headers.get(ACCEPT))

  protected def toResult[T](error: ErrorResponse, acceptHeader: Option[String]): Result = acceptHeader match {
    case Some(VndHmrcXml_1_0) | Some(VndHmrcXml_2_0) =>
      Status(error.httpStatusCode)(<errorResponse><code>{error.errorCode}</code><message>{error.message}</message></errorResponse>).as(MimeTypes.XML)
    case _ => Status(error.httpStatusCode)(Json.toJson(error))

  }
}
