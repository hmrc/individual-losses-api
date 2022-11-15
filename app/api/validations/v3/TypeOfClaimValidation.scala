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

import api.endpoints.lossClaim.domain.v3.TypeOfClaim._
import api.endpoints.lossClaim.domain.v3.TypeOfLoss._
import api.endpoints.lossClaim.domain.v3.{ TypeOfClaim, TypeOfLoss }
import api.models.errors.{ MtdError, RuleTypeOfClaimInvalid, TypeOfClaimFormatError }
import api.validations.NoValidationErrors

object TypeOfClaimValidation {

  def validate(typeOfClaim: String): Seq[MtdError] = {
    if (TypeOfClaim.parser.isDefinedAt(typeOfClaim)) NoValidationErrors else List(TypeOfClaimFormatError)
  }

  def validateTypeOfClaimPermitted(typeOfClaim: TypeOfClaim, typeOfLoss: TypeOfLoss): Seq[MtdError] = {
    val permitted = typeOfLoss match {
      case `self-employment` =>
        typeOfClaim match {
          case `carry-forward` | `carry-sideways` => true
          case _                                  => false
        }

      case `uk-property-non-fhl` | `foreign-property` =>
        typeOfClaim match {
          case `carry-sideways` | `carry-sideways-fhl` | `carry-forward-to-carry-sideways` => true
          case _                                                                           => false
        }
    }

    if (permitted) NoValidationErrors else List(RuleTypeOfClaimInvalid)
  }
}
