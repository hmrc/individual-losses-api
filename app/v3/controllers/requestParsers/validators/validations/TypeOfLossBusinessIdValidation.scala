/*
 * Copyright 2021 HM Revenue & Customs
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

package v3.controllers.requestParsers.validators.validations

import v3.models.domain.TypeOfLoss
import v3.models.errors.{ MtdError, RuleBusinessId }

object TypeOfLossBusinessIdValidation {

  def validate(typeOfLoss: TypeOfLoss, businessId: Option[String]): List[MtdError] = {
    (typeOfLoss, businessId) match {
      case (TypeOfLoss.`uk-property-non-fhl`, None)         => NoValidationErrors
      case (TypeOfLoss.`uk-property-fhl`, None)             => NoValidationErrors
      case (TypeOfLoss.`self-employment`, Some(_))          => NoValidationErrors
      case (TypeOfLoss.`self-employment-class4`, Some(_))   => NoValidationErrors
      case (TypeOfLoss.`foreign-property-fhl-eea`, Some(_)) => NoValidationErrors
      case (TypeOfLoss.`foreign-property`, Some(_))         => NoValidationErrors
      case _                                                => List(RuleBusinessId)
    }
  }

  def validate(typeOfLoss: TypeOfLoss, businessId: String): List[MtdError] = {
    (typeOfLoss, businessId) match {
      case (TypeOfLoss.`uk-property-non-fhl`, _)         => NoValidationErrors
      case (TypeOfLoss.`uk-property-fhl`, _)             => NoValidationErrors
      case (TypeOfLoss.`self-employment`, _)             => NoValidationErrors
      case (TypeOfLoss.`self-employment-class4`, _)      => NoValidationErrors
      case (TypeOfLoss.`foreign-property-fhl-eea`, _)    => NoValidationErrors
      case (TypeOfLoss.`foreign-property`, _)            => NoValidationErrors
      case _                                             => List(RuleBusinessId)
    }
  }
}
