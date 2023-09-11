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

import api.models.domain.BusinessId
import api.models.errors.{BusinessIdFormatError, MtdError}
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}

object ResolveBusinessId extends Resolver[String, BusinessId] {

  private val businessIdRegex = "^X[A-Z0-9]{1}IS[0-9]{11}$".r

  def apply(value: String, unusedError: Option[MtdError], path: Option[String]): Validated[Seq[MtdError], BusinessId] = {
    if (businessIdRegex.matches(value))
      Valid(BusinessId(value))
    else
      Invalid(List(BusinessIdFormatError.maybeWithExtraPath(path)))
  }

}
