/*
 * Copyright 2019 HM Revenue & Customs
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

import v1.models.errors.{MtdError, RuleInvalidSelfEmploymentId, RulePropertySelfEmploymentId}

object SelfEmploymentIdValidation {

  val nonPropertyLossTypes = Seq("self-employment", "self-employment-class4")

  val regex = "^X[A-Z0-9]{1}IS[0-9]{11}$"

  def validate(typeOfLoss: String, selfEmploymentId: Option[String]): List[MtdError] = {
    if (nonPropertyLossTypes.contains(typeOfLoss)) selfEmployedValidation(selfEmploymentId) else propertyValidation(selfEmploymentId)
  }

  private def selfEmployedValidation(selfEmploymentId: Option[String]): List[MtdError] = {
    if (selfEmploymentId.exists(_.matches(regex))) NoValidationErrors else List(RuleInvalidSelfEmploymentId)
  }

  private def propertyValidation(selfEmploymentId: Option[String]): List[MtdError] = {
    if (selfEmploymentId.isEmpty) NoValidationErrors else List(RulePropertySelfEmploymentId)
  }
}