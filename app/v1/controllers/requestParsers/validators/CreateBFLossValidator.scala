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

import v1.controllers.requestParsers.validators.validations._
import v1.models.domain.BFLoss
import v1.models.errors.{MtdError, RuleIncorrectOrEmptyBodyError, RuleInvalidLossAmount, RuleTaxYearNotSupportedError}
import v1.models.requestData.CreateBFLossRawData

class CreateBFLossValidator extends Validator[CreateBFLossRawData] {

  private val validationSet = List(parameterFormatValidation, bodyFormatValidator, bodyFieldsValidator1, bodyFieldsValidator2)

  private def parameterFormatValidation: CreateBFLossRawData => List[List[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino)
    )
  }

  private def bodyFormatValidator: CreateBFLossRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[BFLoss](data.body.json, RuleIncorrectOrEmptyBodyError)
    )
  }


  private def bodyFieldsValidator1: CreateBFLossRawData => List[List[MtdError]] = { data =>
    val req = data.body.json.as[BFLoss]
    List(
      TaxYearValidation.validate(req.taxYear),
      TypeOfLossValidation.validate(req.typeOfLoss)
    )
  }

  private def bodyFieldsValidator2: CreateBFLossRawData => List[List[MtdError]] = { data =>
    val req = data.body.json.as[BFLoss]
    List(
      MtdTaxYearValidation.validate(req.taxYear, RuleTaxYearNotSupportedError),
      SelfEmploymentIdValidation.validate(req.typeOfLoss, req.selfEmploymentId),
      AmountValidation.validate(req.lossAmount)
    )
  }

  override def validate(data: CreateBFLossRawData): List[MtdError] = run(validationSet, data)
}
