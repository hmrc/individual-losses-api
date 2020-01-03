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
import v1.models.domain.TypeOfClaim._
import v1.models.domain.TypeOfLoss._
import v1.models.domain.{TypeOfClaim, TypeOfLoss}
import v1.models.errors.{MtdError, RuleTypeOfClaimInvalid, TypeOfClaimFormatError}

object TypeOfClaimValidation {

  def validate(typeOfClaim: String): List[MtdError] = {
    if (TypeOfClaim.parser.isDefinedAt(typeOfClaim)) NoValidationErrors else List(TypeOfClaimFormatError)
  }

  def checkClaim(typeOfClaim: TypeOfClaim, typeOfLoss: TypeOfLoss): List[MtdError] =
    (typeOfLoss, typeOfClaim) match {
      case (`uk-property-non-fhl`, _) => NoValidationErrors
      case (`self-employment`, `carry-forward`) => NoValidationErrors
      case (`self-employment`, `carry-sideways`) => NoValidationErrors
      case (_,_) => List(RuleTypeOfClaimInvalid)
    }
}
