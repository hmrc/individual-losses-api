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

package api.services.v3

import api.services.anyVersion.Outcomes.ServiceOutcome
import v3.models.response.amendBFLosses.AmendBFLossResponse
import v3.models.response.amendLossClaimType.AmendLossClaimTypeResponse
import v3.models.response.amendLossClaimsOrder.AmendLossClaimsOrderResponse
import v3.models.response.createBFLosses.CreateBFLossResponse
import v3.models.response.createLossClaim.CreateLossClaimResponse
import v3.models.response.listBFLosses.{ListBFLossesItem, ListBFLossesResponse}
import v3.models.response.listLossClaims.{ListLossClaimsItem, ListLossClaimsResponse}
import v3.models.response.retrieveBFLoss.RetrieveBFLossResponse
import v3.models.response.retrieveLossClaim.RetrieveLossClaimResponse

object Outcomes {

  type CreateBFLossOutcome = ServiceOutcome[CreateBFLossResponse]

  type RetrieveBFLossOutcome = ServiceOutcome[RetrieveBFLossResponse]

  type ListBFLossesOutcome = ServiceOutcome[ListBFLossesResponse[ListBFLossesItem]]

  type AmendBFLossOutcome = ServiceOutcome[AmendBFLossResponse]

  type DeleteBFLossOutcome = ServiceOutcome[Unit]

  type CreateLossClaimOutcome = ServiceOutcome[CreateLossClaimResponse]

  type RetrieveLossClaimOutcome = ServiceOutcome[RetrieveLossClaimResponse]

  type ListLossClaimsOutcome = ServiceOutcome[ListLossClaimsResponse[ListLossClaimsItem]]

  type AmendLossClaimTypeOutcome = ServiceOutcome[AmendLossClaimTypeResponse]

  type DeleteLossClaimOutcome = ServiceOutcome[Unit]

  type AmendLossClaimsOrderOutcome = ServiceOutcome[AmendLossClaimsOrderResponse]

}
