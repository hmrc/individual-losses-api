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

package api.endpoints.lossClaim.amendOrder.v3.request

import api.endpoints.lossClaim.domain.v3.TypeOfClaim
import api.models.errors._
import api.validations.v3._
import api.validations.Validator
import api.validations.anyVersion.{JsonFormatValidation, JsonValidation, MinTaxYearValidation, NinoValidation, NumberValidation, SequenceSequentialValidation}
import config.FixedConfig

class AmendLossClaimsOrderValidator extends Validator[AmendLossClaimsOrderRawData] with FixedConfig {

  val validationSet = List(parameterFormatValidation, parameterValueValidation, bodyEnumValidator, bodyFormatValidator, bodyFieldValidator)

  private def parameterFormatValidation: AmendLossClaimsOrderRawData => Seq[Seq[MtdError]] = { data =>
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYearClaimedFor, TaxYearFormatError)
    )
  }

  private def parameterValueValidation: AmendLossClaimsOrderRawData => Seq[Seq[MtdError]] = { data =>
    List(
      MinTaxYearValidation.validate(data.taxYearClaimedFor, minimumTaxYearLossClaim)
    )
  }

  //  Validate body fields (e.g. enums and ranges) that would otherwise fail at JsonFormatValidation with a less specific error
  private def bodyEnumValidator: AmendLossClaimsOrderRawData => Seq[Seq[MtdError]] = { data =>
    List(
      JsonValidation.validate[String](data.body.json \ "typeOfClaim") { value =>
        TypeOfClaim.parser.lift(value) match {
          case Some(TypeOfClaim.`carry-sideways`) => Nil
          case _                                  => List(TypeOfClaimFormatError)
        }
      }
    )
  }

  private def bodyFormatValidator: AmendLossClaimsOrderRawData => Seq[Seq[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendLossClaimsOrderRequestBody](data.body.json)
    )
  }

  private def bodyFieldValidator: AmendLossClaimsOrderRawData => Seq[Seq[MtdError]] = { data =>
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

  override def validate(data: AmendLossClaimsOrderRawData): Seq[MtdError] = run(validationSet, data).distinct
}
