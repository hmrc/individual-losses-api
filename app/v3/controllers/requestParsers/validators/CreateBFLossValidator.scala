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

package v3.controllers.requestParsers.validators

import config.FixedConfig

import javax.inject.{Inject, Singleton}
import utils.CurrentDate
import v3.controllers.requestParsers.validators.validations._
import v3.models.domain.BFLoss
import v3.models.errors.{MtdError, TaxYearFormatError}
import v3.models.requestData.CreateBFLossRawData

@Singleton
class CreateBFLossValidator @Inject()(implicit currentDate: CurrentDate) extends Validator[CreateBFLossRawData] with FixedConfig {

  private val validationSet = List(parameterFormatValidation,
                                   typeOfLossValidator,
                                   bodyFormatValidator,
                                   taxYearValidator,
                                   otherBodyFieldsValidator)

  private def parameterFormatValidation: CreateBFLossRawData => List[List[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino)
    )
  }

  // Validate body fields (e.g. enums) that would otherwise fail at JsonFormatValidation with a less specific error
  private def typeOfLossValidator: CreateBFLossRawData => List[List[MtdError]] = { data =>
    List(
      JsonValidation.validate[String](data.body.json \ "typeOfLoss")(TypeOfBFLossValidation.validate)
    )
  }

  private def bodyFormatValidator: CreateBFLossRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[BFLoss](data.body.json)
    )
  }

  private def taxYearValidator: CreateBFLossRawData => List[List[MtdError]] = { data =>
    val req = data.body.json.as[BFLoss]
    List(
      TaxYearValidation
        .validate(req.taxYearBroughtForwardFrom, TaxYearFormatError)
        .map(
          _.copy(paths = Some(Seq(s"/taxYearBroughtForwardFrom")))
        )
    )
  }

  private def otherBodyFieldsValidator: CreateBFLossRawData => List[List[MtdError]] = { data =>
    val req = data.body.json.as[BFLoss]
    List(
      MinTaxYearValidation.validate(req.taxYearBroughtForwardFrom, minimumTaxYearBFLoss),
      TaxYearNotEndedValidation.validate(req.taxYearBroughtForwardFrom),
      BusinessIdValidation.validate(req.businessId),
      NumberValidation.validate(req.lossAmount, "/lossAmount")
    )
  }

  override def validate(data: CreateBFLossRawData): List[MtdError] = run(validationSet, data)
}
