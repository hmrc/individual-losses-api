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
package v1.controllers.requestParsers.validators

import v1.controllers.requestParsers.validators.validations.{MtdTaxYearValidation, NinoValidation, SelfEmploymentIdValidation, TaxYearValidation}
import v1.models.domain.TypeOfLoss
import v1.models.errors.{MtdError, RuleSelfEmploymentId, RuleTaxYearNotSupportedError, TypeOfLossFormatError}
import v1.models.requestData.ListBFLossesRawData
import TypeOfLoss._

class ListBFLossesValidator extends Validator[ListBFLossesRawData] {

  private val validationSet = List(formatValidation, postFormatValidation)

  // only allow single self employment loss type - so main loss type validator does not quite do it for us
  private val availableLossTypeNames = Seq(`uk-property-fhl`,`uk-property-non-fhl`,`self-employment`).map(_.name)

  private def formatValidation: ListBFLossesRawData => List[List[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      data.taxYear.map(TaxYearValidation.validate).getOrElse(Nil),
      data.typeOfLoss.map(lossType => if (availableLossTypeNames.contains(lossType)) Nil else List(TypeOfLossFormatError)).getOrElse(Nil)
    )
  }

  private def postFormatValidation: ListBFLossesRawData => List[List[MtdError]] = { data =>
    List(
      data.taxYear.map(MtdTaxYearValidation.validate(_, RuleTaxYearNotSupportedError)).getOrElse(Nil),
      data.typeOfLoss.flatMap(TypeOfLoss.parser.lift) match {
        case Some(lossType) => SelfEmploymentIdValidation.validate(lossType, data.selfEmploymentId)
        case None           => if (data.selfEmploymentId.isDefined) List(RuleSelfEmploymentId) else Nil
      }
    )
  }

  override def validate(data: ListBFLossesRawData): List[MtdError] = run(validationSet, data)

}
