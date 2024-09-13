/*
 * Copyright 2024 HM Revenue & Customs
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

package v5.bfLosses.common.resolvers

import cats.data.Validated.{Invalid, Valid}
import cats.data._
import common.errors.TypeOfLossFormatError
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import shared.controllers.validators.resolvers.ResolverSupport
import shared.models.errors.MtdError
import v5.bfLosses.common.domain.TypeOfLoss

object ResolveBFTypeOfLossFromJson extends ResolverSupport {

  def apply(body: JsValue, maybeError: Option[MtdError]): Validated[Seq[MtdError], Option[TypeOfLoss]] = {
    def useError = maybeError.getOrElse(TypeOfLossFormatError)

    val jsPath = body \ "typeOfLoss"

    if (jsPath.isEmpty) {
      Valid(None)
    } else {
      jsPath.validate[String] match {
        case JsError(_) => Invalid(List(useError))
        case JsSuccess(value, _) =>
          TypeOfLoss.parser
            .lift(value)
            .map(parsed => Valid(Some(parsed)))
            .getOrElse(Invalid(List(useError)))
      }
    }
  }

}
