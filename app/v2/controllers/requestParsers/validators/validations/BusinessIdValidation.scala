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

package v2.controllers.requestParsers.validators.validations

import v2.models.domain.TypeOfLoss
import v2.models.errors.{BusinessIdFormatError, MtdError, RuleBusinessId}

object BusinessIdValidation {

  private val regex = "^X[A-Z0-9]{1}IS[0-9]{11}$"

  def validate(typeOfLoss: TypeOfLoss, businessId: String): List[MtdError] = {
    if (typeOfLoss.isUkProperty) propertyValidation(businessId) else businessIdValidation(businessId)
  }

  def validateOptional(typeOfLoss: TypeOfLoss, businessId: Option[String], idOptional: Boolean = false): List[MtdError] = {
    if (typeOfLoss.isUkProperty) propertyValidationOptional(businessId) else businessIdValidationOptional(businessId, idOptional)
  }

  private def businessIdValidationOptional(businessId: Option[String], idOptional: Boolean): List[MtdError] = {
    businessId match {
      case None if idOptional => Nil
      case None               => List(RuleBusinessId)
      case Some(id)           => if (id.matches(regex)) NoValidationErrors else List(BusinessIdFormatError)
    }
  }

  def businessIdValidation(businessId: String): List[MtdError] = {
    if (businessId.matches(regex)) NoValidationErrors else List(BusinessIdFormatError)
  }

  private def propertyValidationOptional(businessId: Option[String]): List[MtdError] = {
    if (businessId.isEmpty) NoValidationErrors else List(RuleBusinessId)
  }

  private def propertyValidation(businessId: String): List[MtdError] = {
    if (businessId.isEmpty) NoValidationErrors else List(RuleBusinessId)
  }
}