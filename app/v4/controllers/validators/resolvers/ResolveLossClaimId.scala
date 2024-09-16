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

package v4.controllers.validators.resolvers

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import common.errors.ClaimIdFormatError
import shared.controllers.validators.resolvers.ResolverSupport
import shared.models.errors.MtdError
import v4.models.domain.lossClaim.ClaimId

import scala.util.matching.Regex

object ResolveLossClaimId extends ResolverSupport {

  private val regexFormat: Regex = "^[A-Za-z0-9]{15}$".r

  def apply(value: String, maybeError: Option[MtdError] = None): Validated[Seq[MtdError], ClaimId] =
    if (regexFormat.matches(value))
      Valid(ClaimId(value))
    else
      Invalid(List(maybeError.getOrElse(ClaimIdFormatError)))

}
