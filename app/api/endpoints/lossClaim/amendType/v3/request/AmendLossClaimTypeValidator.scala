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

package api.endpoints.lossClaim.amendType.v3.request

import api.models.errors.MtdError
import api.validations.v3.{ClaimIdValidation, TypeOfClaimValidation}
import api.validations.Validator
import api.validations.anyVersion.{JsonFormatValidation, JsonValidation, NinoValidation}

class AmendLossClaimTypeValidator extends Validator[AmendLossClaimTypeRawData] {

  val validationSet = List(parameterFormatValidation, typeOfClaimValidator, bodyFormatValidator)

  private def parameterFormatValidation: AmendLossClaimTypeRawData => Seq[Seq[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      ClaimIdValidation.validate(data.claimId)
    )
  }

  private def bodyFormatValidator: AmendLossClaimTypeRawData => Seq[Seq[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendLossClaimTypeRequestBody](data.body.json)
    )
  }

  private def typeOfClaimValidator: AmendLossClaimTypeRawData => Seq[Seq[MtdError]] = { data =>
    List(
      JsonValidation.validate[String](data.body.json \ "typeOfClaim")(TypeOfClaimValidation.validate)
    )
  }

  override def validate(data: AmendLossClaimTypeRawData): Seq[MtdError] = run(validationSet, data)
}
