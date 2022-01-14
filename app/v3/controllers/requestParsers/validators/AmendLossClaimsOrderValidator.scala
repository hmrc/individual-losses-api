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
import v3.controllers.requestParsers.validators.validations._
import v3.models.domain.{AmendLossClaimsOrderRequestBody, TypeOfClaim}
import v3.models.errors.{MtdError, TaxYearFormatError, TypeOfClaimFormatError}
import v3.models.requestData.AmendLossClaimsOrderRawData

class AmendLossClaimsOrderValidator extends Validator[AmendLossClaimsOrderRawData] with FixedConfig {

  val validationSet = List(parameterFormatValidation, parameterValueValidation, bodyEnumValidator, bodyFormatValidator, bodyFieldValidator)

  private def parameterFormatValidation: AmendLossClaimsOrderRawData => List[List[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYearClaimedFor, TaxYearFormatError)
    )
  }

  private def parameterValueValidation: AmendLossClaimsOrderRawData => List[List[MtdError]] = { data =>
    List(
      MinTaxYearValidation.validate(data.taxYearClaimedFor, minimumTaxYearLossClaim)
    )
  }

  //  Validate body fields (e.g. enums and ranges) that would otherwise fail at JsonFormatValidation with a less specific error
  private def bodyEnumValidator: AmendLossClaimsOrderRawData => List[List[MtdError]] = { data =>
    List(
      JsonValidation.validate[String](data.body.json \ "claimType") { value =>
        TypeOfClaim.parser.lift(value) match {
          case Some(TypeOfClaim.`carry-sideways`) => Nil
          case _                                  => List(TypeOfClaimFormatError)
        }
      }
    )
  }

  private def bodyFormatValidator: AmendLossClaimsOrderRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendLossClaimsOrderRequestBody](data.body.json)
    )
  }

  private def bodyFieldValidator: AmendLossClaimsOrderRawData => List[List[MtdError]] = { data =>
    val req = data.body.json.as[AmendLossClaimsOrderRequestBody]

    val listOfLossClaimsValidator = req.listOfLossClaims.zipWithIndex.flatMap {
      case (lossClaim, index) =>
        List(
          ClaimIdValidation.validate(lossClaim.claimId, path = s"/listOfLossClaims/$index/claimId"),
          NumberValidation.validate(lossClaim.sequence, path = s"/listOfLossClaims/$index/sequence", min = 1, max = 99)
        )
    }

    val sequenceValidation = List(
      SequenceSequentialValidation.validate(req.listOfLossClaims.map(_.sequence))
    )

    sequenceValidation ++ listOfLossClaimsValidator
  }

  override def validate(data: AmendLossClaimsOrderRawData): List[MtdError] = run(validationSet, data).distinct
}
