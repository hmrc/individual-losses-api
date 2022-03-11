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

package v2.controllers.requestParsers.validators

import api.models.errors.MtdError
import api.validations.NinoValidation
import v2.models.domain.AmendLossClaim
import v2.models.errors.RuleIncorrectOrEmptyBodyError
import v2.models.requestData.AmendLossClaimRawData
import v2.validations.{ClaimIdValidation, JsonFormatValidation, JsonValidation, TypeOfClaimValidation}

class AmendLossClaimValidator extends Validator[AmendLossClaimRawData] {

  val validationSet = List(parameterFormatValidation, typeOfClaimValidator, bodyFormatValidator)

  private def parameterFormatValidation: AmendLossClaimRawData => List[List[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      ClaimIdValidation.validate(data.claimId)
    )
  }

  private def bodyFormatValidator: AmendLossClaimRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendLossClaim](data.body.json, RuleIncorrectOrEmptyBodyError)
    )
  }

  private def typeOfClaimValidator: AmendLossClaimRawData => List[List[MtdError]] = { data =>
    List(
      JsonValidation.validate[String](data.body.json \ "typeOfClaim")(TypeOfClaimValidation.validate)
    )
  }

  override def validate(data: AmendLossClaimRawData): List[MtdError] = run(validationSet, data)
}
