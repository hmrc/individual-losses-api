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

package api.endpoints.bfLoss.amend.v3.request

import api.endpoints.bfLoss.amend.anyVersion.request.{AmendBFLossRawData, AmendBFLossRequestBody}
import api.models.errors.MtdError
import api.validations.v3.{JsonFormatValidation, LossIdValidation, NumberValidation}
import api.validations.{NinoValidation, Validator}

import javax.inject.Singleton

@Singleton
class AmendBFLossValidator extends Validator[AmendBFLossRawData] {

  private val validationSet = List(parameterFormatValidation, bodyFormatValidator, bodyFieldsValidator)

  private def parameterFormatValidation: AmendBFLossRawData => Seq[Seq[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      LossIdValidation.validate(data.lossId)
    )
  }

  private def bodyFormatValidator: AmendBFLossRawData => Seq[Seq[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendBFLossRequestBody](data.body.json)
    )
  }

  private def bodyFieldsValidator: AmendBFLossRawData => Seq[Seq[MtdError]] = { data =>
    val req = data.body.json.as[AmendBFLossRequestBody]
    List(
      NumberValidation.validate(req.lossAmount, "/lossAmount")
    )
  }

  override def validate(data: AmendBFLossRawData): Seq[MtdError] = run(validationSet, data)
}
