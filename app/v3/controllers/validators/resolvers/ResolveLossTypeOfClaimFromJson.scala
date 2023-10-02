/*
 * Copyright 2023 HM Revenue & Customs
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

package v3.controllers.validators.resolvers

import api.controllers.validators.resolvers.Resolver
import api.models.errors.{MtdError, TypeOfClaimFormatError}
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import v3.models.domain.lossClaim.TypeOfClaim

/** Given the whole request body, checks for the typeOfClaim field and validates it if present.
  */
object ResolveLossTypeOfClaimFromJson extends Resolver[JsValue, Option[TypeOfClaim]] {

  override def apply(body: JsValue, error: Option[MtdError], errorPath: Option[String]): Validated[Seq[MtdError], Option[TypeOfClaim]] = {
    def useError = error.getOrElse(TypeOfClaimFormatError).maybeWithExtraPath(errorPath)

    val jspath = body \ "typeOfClaim"

    if (jspath.isEmpty) Valid(None)
    else {
      jspath.validate[String] match {
        case JsError(_) => Invalid(List(useError))
        case JsSuccess(value, _) =>
          TypeOfClaim.parser
            .lift(value)
            .map(parsed => Valid(Some(parsed)))
            .getOrElse(Invalid(List(useError)))
      }
    }

  }

}
