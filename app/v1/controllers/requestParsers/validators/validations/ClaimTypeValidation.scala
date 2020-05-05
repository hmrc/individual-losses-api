/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.controllers.requestParsers.validators.validations

import v1.models.domain.TypeOfClaim
import v1.models.errors.{ClaimTypeFormatError, MtdError}

object ClaimTypeValidation {
  def validate(claimType: String): List[MtdError] =
    if (TypeOfClaim.parser.isDefinedAt(claimType)) NoValidationErrors else List(ClaimTypeFormatError)

  def validateClaimIsCarrySideways(typeOfClaim: String): List[MtdError] =
    if(typeOfClaim == TypeOfClaim.`carry-sideways`.toString) {
      NoValidationErrors
    }
    else {
      List(ClaimTypeFormatError)
    }
}