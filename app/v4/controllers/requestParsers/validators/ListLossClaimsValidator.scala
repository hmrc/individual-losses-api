/*
 * Copyright 2023 HM Revenue & Customs
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

package v4.controllers.requestParsers.validators

import api.controllers.requestParsers.validators.Validator
import api.models.errors._
import api.controllers.requestParsers.validators.validations.{
  BusinessIdValidation,
  MinTaxYearValidation,
  NinoValidation,
  TaxYearValidation,
  TypeOfClaimLossValidation
}
import config.FixedConfig
import v4.models.domain.lossClaim.TypeOfClaim
import v4.models.request.listLossClaims.ListLossClaimsRawData

class ListLossClaimsValidator extends Validator[ListLossClaimsRawData] with FixedConfig {

  private val validationSet = List(formatValidation, postFormatValidation)

  private def formatValidation: ListLossClaimsRawData => Seq[Seq[MtdError]] =
    data =>
      List(
        NinoValidation.validate(data.nino),
        data.businessId.map(BusinessIdValidation.validate).getOrElse(Nil),
        TaxYearValidation.validate(data.taxYearClaimedFor, TaxYearFormatError),
        data.typeOfLoss.map(TypeOfClaimLossValidation.validate).getOrElse(Nil),
        data.typeOfClaim.map(validateClaimType).getOrElse(Nil)
      )

  private def postFormatValidation: ListLossClaimsRawData => Seq[Seq[MtdError]] = { data =>
    List(
      MinTaxYearValidation.validate(data.taxYearClaimedFor, minimumTaxYearLossClaim)
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
