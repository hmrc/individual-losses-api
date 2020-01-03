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
import v1.models.domain.LossClaim
import v1.models.errors.{MtdError, RuleIncorrectOrEmptyBodyError}
import v1.models.requestData.CreateLossClaimRawData


class CreateLossClaimValidator extends Validator[CreateLossClaimRawData] with FixedConfig{

  private val validationSet = List(parameterFormatValidation, typeOfLossValidator, typeOfClaimValidator,
    bodyFormatValidator, taxYearValidator, otherBodyFieldsValidator)


  private def parameterFormatValidation: CreateLossClaimRawData => List[List[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino)
    )
  }

  //  Validate body fields (e.g. enums) that would otherwise fail at JsonFormatValidation with a less specific error
  private def typeOfLossValidator: CreateLossClaimRawData => List[List[MtdError]] = { data =>
    List(
      JsonValidation.validate[String](data.body.json \ "typeOfLoss")(TypeOfLossValidation.validateLossClaim)
    )
  }

  // Validate body fields (e.g. enums) that would otherwise fail at JsonFormatValidation with a less specific error
  private def typeOfClaimValidator: CreateLossClaimRawData => List[List[MtdError]] = { data =>
    List(
      JsonValidation.validate[String](data.body.json \ "typeOfClaim")(TypeOfClaimValidation.validate)
    )
  }

  private def bodyFormatValidator: CreateLossClaimRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[LossClaim](data.body.json, RuleIncorrectOrEmptyBodyError)
    )
  }

  private def taxYearValidator: CreateLossClaimRawData => List[List[MtdError]] = { data =>
    val req = data.body.json.as[LossClaim]
    List(
      TaxYearValidation.validate(req.taxYear)
    )
  }

  private def otherBodyFieldsValidator: CreateLossClaimRawData => List[List[MtdError]] = { data =>
    val req = data.body.json.as[LossClaim]
    List(
      MinTaxYearValidation.validate(req.taxYear, minimumTaxYearLossClaim),
      SelfEmploymentIdValidation.validate(req.typeOfLoss, req.selfEmploymentId),
      TypeOfClaimValidation.checkClaim(req.typeOfClaim, req.typeOfLoss)
    )
  }

  override def validate(data: CreateLossClaimRawData): List[MtdError] = run(validationSet, data)
}
