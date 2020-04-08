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

import config.FixedConfig
import v1.controllers.requestParsers.validators.validations._
import v1.models.domain.TypeOfLoss
import v1.models.errors.MtdError
import v1.models.requestData.ListLossClaimsRawData

class ListLossClaimsValidator extends Validator[ListLossClaimsRawData] with FixedConfig {

  private val validationSet = List(formatValidation, postFormatValidation)

  private def formatValidation: ListLossClaimsRawData => List[List[MtdError]] = data => List(
    NinoValidation.validate(data.nino),
    data.taxYear.map(TaxYearValidation.validate).getOrElse(Nil),
    data.typeOfLoss.map(TypeOfLossValidation.validateLossClaim).getOrElse(Nil)
  )

  private def postFormatValidation: ListLossClaimsRawData => List[List[MtdError]] = { data =>
    List(
      data.taxYear.map(MinTaxYearValidation.validate(_, minimumTaxYearLossClaim)).getOrElse(Nil),
      data.typeOfLoss.flatMap(TypeOfLoss.parser.lift) match {
        case Some(lossType) => SelfEmploymentIdValidation.validate(lossType, data.selfEmploymentId, idOptional = true)
        case None           => Nil
      },
      data.claimType.map(ClaimTypeValidation.validate).getOrElse(Nil)
    )
  }

  override def validate(data: ListLossClaimsRawData): List[MtdError] = run(validationSet, data)
}
