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

package api.endpoints.bfLoss.list.v3.request

import api.endpoints.bfLoss.domain.v3.TypeOfLoss._
import api.models.errors._
import api.validations.v3.{BusinessIdValidation, TaxYearValidation}
import api.validations.Validator
import api.validations.anyVersion.{MinTaxYearValidation, NinoValidation}
import config.FixedConfig

class ListBFLossesValidator extends Validator[ListBFLossesRawData] with FixedConfig {

  private val validationSet = List(formatValidation, postFormatValidation)

  // only allow single self employment loss type - so main loss type validator does not quite do it for us
  private val availableLossTypeNames =
    Seq(`uk-property-fhl`, `uk-property-non-fhl`, `self-employment`, `foreign-property-fhl-eea`, `foreign-property`).map(_.toString)

  private def formatValidation: ListBFLossesRawData => Seq[Seq[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      data.taxYearBroughtForwardFrom.map(TaxYearValidation.validate(_, TaxYearFormatError)).getOrElse(Nil),
      data.businessId.map(BusinessIdValidation.validate).getOrElse(Nil),
      data.typeOfLoss.map(lossType => if (availableLossTypeNames.contains(lossType)) Nil else List(TypeOfLossFormatError)).getOrElse(Nil)
    )
  }

  private def postFormatValidation: ListBFLossesRawData => Seq[Seq[MtdError]] = { data =>
    List(
      data.taxYearBroughtForwardFrom.map(MinTaxYearValidation.validate(_, minimumTaxYearBFLoss)).getOrElse(Nil),
    )
  }

  override def validate(data: ListBFLossesRawData): Seq[MtdError] = run(validationSet, data)

}