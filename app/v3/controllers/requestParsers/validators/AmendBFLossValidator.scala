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

import v3.controllers.requestParsers.validators.validations.{JsonFormatValidation, LossIdValidation, NinoValidation, NumberValidation}
import v3.models.domain.AmendBFLoss
import v3.models.errors.MtdError
import v3.models.requestData.AmendBFLossRawData

class AmendBFLossValidator extends Validator[AmendBFLossRawData] {

  private val validationSet = List(parameterFormatValidation, bodyFormatValidator, bodyFieldsValidator)

  private def parameterFormatValidation: AmendBFLossRawData => List[List[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      LossIdValidation.validate(data.lossId)
    )
  }

  private def bodyFormatValidator: AmendBFLossRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendBFLoss](data.body.json)
    )
  }

  private def bodyFieldsValidator: AmendBFLossRawData => List[List[MtdError]] = { data =>
    val req = data.body.json.as[AmendBFLoss]
    List(
      NumberValidation.validate(req.lossAmount, "/lossAmount")
    )
  }

  override def validate(data: AmendBFLossRawData): List[MtdError] = run(validationSet, data)
}
