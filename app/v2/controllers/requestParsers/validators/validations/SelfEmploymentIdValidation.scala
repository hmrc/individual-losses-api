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

package v2.controllers.requestParsers.validators.validations

import v2.models.domain.TypeOfLoss
import v2.models.errors.{BusinessIdFormatError, MtdError, RuleBusinessId}

object SelfEmploymentIdValidation {

  private val regex = "^X[A-Z0-9]{1}IS[0-9]{11}$"

  def validate(typeOfLoss: TypeOfLoss, businessId: Option[String], idOptional: Boolean = false): List[MtdError] = {
    if (typeOfLoss.isUkProperty) propertyValidation(businessId) else selfEmployedValidation(businessId, idOptional)
  }

  private def selfEmployedValidation(businessId: Option[String], idOptional: Boolean): List[MtdError] = {
    businessId match {
      case None if idOptional => Nil
      case None               => List(RuleBusinessId)
      case Some(id)           => if (id.matches(regex)) NoValidationErrors else List(BusinessIdFormatError)
    }
  }

  private def propertyValidation(businessId: Option[String]): List[MtdError] = {
    if (businessId.isEmpty) NoValidationErrors else List(RuleBusinessId)
  }
}