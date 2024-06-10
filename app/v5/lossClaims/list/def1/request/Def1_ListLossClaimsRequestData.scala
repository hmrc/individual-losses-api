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

package v5.lossClaims.list.def1.request

import api.models.domain.{BusinessId, Nino, TaxYear}
import v4.models.domain.lossClaim.{TypeOfClaim, TypeOfLoss}
import v5.lossClaims.list.ListLossClaimsSchema
import v5.lossClaims.list.model.request.ListLossClaimsRequestData

case class Def1_ListLossClaimsRequestData(nino: Nino,
                                          taxYearClaimedFor: TaxYear,
                                          typeOfLoss: Option[TypeOfLoss],
                                          businessId: Option[BusinessId],
                                          typeOfClaim: Option[TypeOfClaim])
    extends ListLossClaimsRequestData {
  val schema: ListLossClaimsSchema = ListLossClaimsSchema.Def1

}
