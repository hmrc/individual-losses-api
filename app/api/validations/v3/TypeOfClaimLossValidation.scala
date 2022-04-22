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

import api.endpoints.common.lossClaim.v3.domain.TypeOfLoss
import api.models.errors.{ MtdError, TypeOfLossFormatError }
import api.validations.NoValidationErrors

object TypeOfClaimLossValidation {

  def validate(typeOfLoss: String): List[MtdError] = {
    if (TypeOfLoss.parser.isDefinedAt(typeOfLoss)) NoValidationErrors else List(TypeOfLossFormatError)
  }

}
