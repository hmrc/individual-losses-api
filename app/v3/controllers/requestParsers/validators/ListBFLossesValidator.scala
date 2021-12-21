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

package v3.controllers.requestParsers.validators

import config.FixedConfig
import v3.controllers.requestParsers.validators.validations.{MinTaxYearValidation, NinoValidation, BusinessIdValidation, TaxYearValidation}
import v3.models.domain.TypeOfLoss
import v3.models.domain.TypeOfLoss._
import v3.models.errors.{MtdError, TypeOfLossFormatError}
import v3.models.requestData.ListBFLossesRawData

class ListBFLossesValidator extends Validator[ListBFLossesRawData] with FixedConfig {

  private val validationSet = List(formatValidation, postFormatValidation)

  // only allow single self employment loss type - so main loss type validator does not quite do it for us
  private val availableLossTypeNames = Seq(`uk-property-fhl`,`uk-property-non-fhl`,`self-employment`,`foreign-property-fhl-eea`,`foreign-property`).map(_.toString)

  private def formatValidation: ListBFLossesRawData => List[List[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      data.taxYearBroughtForwardFrom.map(TaxYearValidation.validate).getOrElse(Nil),
      data.typeOfLoss.map(lossType => if (availableLossTypeNames.contains(lossType)) Nil else List(TypeOfLossFormatError)).getOrElse(Nil)
    )
  }

  private def postFormatValidation: ListBFLossesRawData => List[List[MtdError]] = { data =>
    List(
      data.taxYearBroughtForwardFrom.map(MinTaxYearValidation.validate(_, minimumTaxYearBFLoss)).getOrElse(Nil),
      data.typeOfLoss.flatMap(TypeOfLoss.parser.lift) match {
        case Some(lossType) => BusinessIdValidation.validateOptionalWithTypeOfLoss(lossType, data.businessId, idOptional = true, noRuleBusinessIdError = true)
        case None           => Nil
      }
    )
  }

  override def validate(data: ListBFLossesRawData): List[MtdError] = run(validationSet, data)

}
