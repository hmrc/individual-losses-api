/*
 * Copyright 2023 HM Revenue & Customs
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

package v3.controllers.requestParsers.validators

import api.controllers.requestParsers.validators.Validator
import api.models.errors.MtdError
import api.controllers.requestParsers.validators.validations.{ClaimIdValidation, NinoValidation}
import v3.models.request.retrieveLossClaim.RetrieveLossClaimRawData

class RetrieveLossClaimValidator extends Validator[RetrieveLossClaimRawData] {

  private val validationSet = List(parameterFormatValidation)

  private def parameterFormatValidation: RetrieveLossClaimRawData => Seq[Seq[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      ClaimIdValidation.validate(data.claimId)
    )
  }

  override def validate(data: RetrieveLossClaimRawData): Seq[MtdError] = run(validationSet, data)
}