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

package api.endpoints.lossClaim.create.v3.request

import api.models.errors.MtdError
import api.models.errors.v3.TaxYearClaimedForFormatError
import api.validations.v3._
import api.validations.{JsonValidation, NinoValidation, Validator}
import config.FixedConfig

class CreateLossClaimValidator extends Validator[CreateLossClaimRawData] with FixedConfig {

  private val validationSet = List(
    parameterFormatValidation,
    enumValidator,
    bodyFormatValidator,
    taxYearValidator,
    otherBodyFieldsValidator
  )

  private def parameterFormatValidation: CreateLossClaimRawData => Seq[Seq[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino)
    )
  }

  //  Validate body fields (e.g. enums) that would otherwise fail at JsonFormatValidation with a less specific error
  private def enumValidator: CreateLossClaimRawData => Seq[Seq[MtdError]] = { data =>
    List(
      JsonValidation.validate[String](data.body.json \ "typeOfLoss")(TypeOfClaimLossValidation.validate),
      JsonValidation.validate[String](data.body.json \ "typeOfClaim")(TypeOfClaimValidation.validate)
    )
  }

  private def bodyFormatValidator: CreateLossClaimRawData => Seq[Seq[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[CreateLossClaimRequestBody](data.body.json)
    )
  }

  private def taxYearValidator: CreateLossClaimRawData => Seq[Seq[MtdError]] = { data =>
    val req = data.body.json.as[CreateLossClaimRequestBody]
    List(
      TaxYearValidation
        .validate(req.taxYearClaimedFor, TaxYearClaimedForFormatError)
        .map(
          _.copy(paths = Some(Seq(s"/taxYearClaimedFor")))
        )
    )
  }

  private def otherBodyFieldsValidator: CreateLossClaimRawData => Seq[Seq[MtdError]] = { data =>
    val req = data.body.json.as[CreateLossClaimRequestBody]
    List(
      MinTaxYearValidation.validate(req.taxYearClaimedFor, minimumTaxYearLossClaim),
      BusinessIdValidation.validate(req.businessId),
      TypeOfClaimValidation.validateTypeOfClaimPermitted(req.typeOfClaim, req.typeOfLoss)
    )
  }

  override def validate(data: CreateLossClaimRawData): Seq[MtdError] = run(validationSet, data)
}
