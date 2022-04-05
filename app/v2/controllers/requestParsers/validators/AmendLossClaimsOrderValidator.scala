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

package v2.controllers.requestParsers.validators

import api.models.domain.v2.AmendLossClaimsOrderRequestBody
import api.models.errors.{ MtdError, RuleIncorrectOrEmptyBodyError }
import api.validations.v2._
import api.validations.{ NinoValidation, Validator }
import v2.models.requestData.AmendLossClaimsOrderRawData

class AmendLossClaimsOrderValidator extends Validator[AmendLossClaimsOrderRawData] {

  val validationSet = List(parameterFormatValidation, bodyFormatValidator, bodyFieldValidator)

  private def parameterFormatValidation: AmendLossClaimsOrderRawData => List[List[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      data.taxYear.map(ClaimOrderTaxYearValidation.validate).getOrElse(Nil)
    )
  }

  private def bodyFormatValidator: AmendLossClaimsOrderRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendLossClaimsOrderRequestBody](data.body.json, RuleIncorrectOrEmptyBodyError)
    )
  }

  private def bodyFieldValidator: AmendLossClaimsOrderRawData => List[List[MtdError]] = { data =>
    val req = data.body.json.as[AmendLossClaimsOrderRequestBody]
    val claimTypeValidation = List(
      ClaimTypeValidation.validateClaimIsCarrySideways(req.claimType)
    )
    val listOfLossClaimsValidator = req.listOfLossClaims.flatMap { lossClaim =>
      List(
        ClaimIdValidation.validate(lossClaim.id),
        SequenceValidation.validate(lossClaim.sequence)
      )
    }
    List(
      SequenceSequentialValidation.validate(req.listOfLossClaims.map(_.sequence))
    ) ++ claimTypeValidation ++ listOfLossClaimsValidator
  }

  override def validate(data: AmendLossClaimsOrderRawData): List[MtdError] = run(validationSet, data).distinct
}
