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

package api.validations.v3

import api.models.errors.MtdError
import api.validations.NoValidationErrors

trait RegexValidation {
  protected val regexFormat: String

  protected val error: MtdError

  def validate(value: String): Seq[MtdError] =
    RegexValidation.validate(error, value, regexFormat)

  def validate(value: String, path: String): Seq[MtdError] =
    RegexValidation.validate(error.copy(paths = Some(Seq(path))), value, regexFormat)
}

object RegexValidation {

  private def validate(error: => MtdError, value: String, regexFormat: String): Seq[MtdError] = {
    if (value.matches(regexFormat)) NoValidationErrors else List(error)
  }
}
