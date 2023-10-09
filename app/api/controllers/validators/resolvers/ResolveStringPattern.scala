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

package api.controllers.validators.resolvers

import api.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

import scala.util.matching.Regex

trait StringPatternResolving extends Resolver[String, String] {

  protected val regexFormat: Regex
  protected val error: MtdError

  protected def resolve(value: String, maybeError: Option[MtdError], path: Option[String]): Validated[Seq[MtdError], String] =
    if (regexFormat.matches(value))
      Valid(value)
    else
      Invalid(List(maybeError.getOrElse(error).maybeWithExtraPath(path)))

}

class ResolveStringPattern(protected val regexFormat: Regex, protected val error: MtdError) extends StringPatternResolving {

  def apply(value: String, maybeError: Option[MtdError], path: Option[String]): Validated[Seq[MtdError], String] =
    resolve(value, maybeError, path)

}
