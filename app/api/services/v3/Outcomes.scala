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

import api.endpoints.bfLoss.amend.v3.response.AmendBFLossResponse
import api.endpoints.bfLoss.create.v3.response.CreateBFLossResponse
import api.endpoints.bfLoss.list.v3.response.{ ListBFLossesItem, ListBFLossesResponse }
import api.endpoints.bfLoss.retrieve.v3.response.RetrieveBFLossResponse
import api.endpoints.lossClaim.amendOrder.v3.response.AmendLossClaimsOrderResponse
import api.endpoints.lossClaim.amendType.v3.response.AmendLossClaimTypeResponse
import api.endpoints.lossClaim.create.v3.response.CreateLossClaimResponse
import api.endpoints.lossClaim.list.v3.response.{ ListLossClaimsItem, ListLossClaimsResponse }
import api.endpoints.lossClaim.retrieve.v3.response.RetrieveLossClaimResponse
import api.services.anyVersion.Outcomes.ServiceOutcome

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
