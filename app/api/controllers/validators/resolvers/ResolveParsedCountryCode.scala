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

import api.models.errors.{CountryCodeFormatError, MtdError, RuleCountryCodeError}
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import com.neovisionaries.i18n.CountryCode

object ResolveParsedCountryCode extends Resolver[String, String] {

  private val permittedCustomCodes = Set("ZZZ")

  def apply(value: String, notUsedError: Option[MtdError], path: Option[String]): Validated[Seq[MtdError], String] = {
    if (value.length != 3) {
      Invalid(List(CountryCodeFormatError.maybeWithPath(path)))
    } else if (permittedCustomCodes.contains(value)) {
      Valid(value)
    } else {
      Option(CountryCode.getByAlpha3Code(value)) match {
        case Some(_) => Valid(value)
        case None    => Invalid(List(RuleCountryCodeError.maybeWithPath(path)))
      }
    }
  }

}
