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

package api.models.errors

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class MtdError(code: String, message: String, httpStatus: Int = 0, paths: Option[Seq[String]] = None)

object MtdError {

  implicit val writes: OWrites[MtdError] = (
    (JsPath \ "code").write[String] and
      (JsPath \ "message").write[String] and
      (JsPath \ "paths").writeNullable[Seq[String]]
  )(unlift(MtdError.unapply))

  // excludes httpStatus
  def unapply(e: MtdError): Option[(String, String, Option[Seq[String]])] = Some((e.code, e.message, e.paths))

  implicit def genericWrites[T <: MtdError]: OWrites[T] =
    writes.contramap[T](c => c: MtdError)

  implicit val reads: Reads[MtdError] = (
    (__ \ "code").read[String] and
      (__ \ "reason").read[String] and
      (__ \ "httpStatus").read(0) and // downstream response doesn't have this field
      Reads.pure(None)
  )(MtdError.apply _)
}

object MtdErrorWithCode {
  def unapply(arg: MtdError): Option[String] = Some(arg.code)
}
