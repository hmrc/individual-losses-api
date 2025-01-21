/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.list

import shared.controllers.validators.Validator
import v6.lossClaims.list.ListLossClaimsSchema.Def1
import v6.lossClaims.list.def1.Def1_ListLossClaimsValidator
import v6.lossClaims.list.model.request.ListLossClaimsRequestData

class ListLossClaimsValidatorFactory {

  def validator(
      nino: String,
      taxYearClaimedFor: String,
      typeOfLoss: Option[String],
      businessId: Option[String],
      typeOfClaim: Option[String]
  ): Validator[ListLossClaimsRequestData] = {

    ListLossClaimsSchema.schema match {
      case Def1 => new Def1_ListLossClaimsValidator(nino, taxYearClaimedFor, typeOfLoss, businessId, typeOfClaim)
    }
  }

}
