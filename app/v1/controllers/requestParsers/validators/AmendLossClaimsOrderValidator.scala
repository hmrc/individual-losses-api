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

package v1.controllers.requestParsers.validators

import v1.controllers.requestParsers.validators.validations.{ClaimIdValidation, ClaimTypeValidation, JsonFormatValidation, JsonValidation, NinoValidation, TaxYearValidation}
import v1.models.errors.{MtdError, RuleIncorrectOrEmptyBodyError}

class AmendLossClaimsOrderValidator extends Validator[???] {

  val validationSet = List(parameterFormatValidation, bodyFieldValidator, bodyFormatValidator)

  private def parameterFormatValidation: ??? => List[List[MtdError]] = { data =>
  List(
    NinoValidation.validate(data.nino),
    TaxYearValidation.validate(???)
  )
}

  private def bodyFormatValidator: ??? => List[List[MtdError]] = { data =>
  List(
    JsonFormatValidation.validate[???](data.body.json, RuleIncorrectOrEmptyBodyError)
  )
}

  private def bodyFieldValidator: ??? => List[List[MtdError]] = { data =>
  List(
    JsonValidation.validate[String](data.body.json \ "claimType")(ClaimTypeValidation.validate),
    JsonValidation.validate[String](data.body.json \ "claim" \ "id")(ClaimIdValidation.validate),
    JsonValidation.validate[String](data.body.json \ "claim" \ "sequence")(???.validate)
  )
}

  override def validate(data: ???): List[MtdError] = run(validationSet, data)
}
