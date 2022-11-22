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

package api.endpoints.lossClaim.list.v3.request

import api.endpoints.lossClaim.domain.v3.TypeOfClaim
import api.models.errors._
import api.validations.Validator
import api.validations.anyVersion.{MinTaxYearValidation, NinoValidation}
import api.validations.v3.{BusinessIdValidation, TaxYearValidation, TypeOfClaimLossValidation}
import config.FixedConfig

class ListLossClaimsValidator extends Validator[ListLossClaimsRawData] with FixedConfig {

  private val validationSet = List(formatValidation, postFormatValidation)

  private def formatValidation: ListLossClaimsRawData => Seq[Seq[MtdError]] =
    data =>
      List(
        NinoValidation.validate(data.nino),
        data.businessId.map(BusinessIdValidation.validate).getOrElse(Nil),
        data.taxYearClaimedFor.map(TaxYearValidation.validate(_, TaxYearFormatError)).getOrElse(Nil),
        data.typeOfLoss.map(TypeOfClaimLossValidation.validate).getOrElse(Nil),
        data.typeOfClaim.map(validateClaimType).getOrElse(Nil)
    )

  private def postFormatValidation: ListLossClaimsRawData => Seq[Seq[MtdError]] = { data =>
    List(
      data.taxYearClaimedFor.map(MinTaxYearValidation.validate(_, minimumTaxYearLossClaim)).getOrElse(Nil),
    )
  }

  // Allow only carry-sideways here...
  private def validateClaimType(claimType: String) =
    TypeOfClaim.parser.lift(claimType) match {
      case Some(TypeOfClaim.`carry-sideways`) => Nil
      case _                                  => List(TypeOfClaimFormatError)
    }

  override def validate(data: ListLossClaimsRawData): Seq[MtdError] = run(validationSet, data)
}
