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

package v3

import api.endpoints.bfLoss.amend.v3.response.AmendBFLossResponse
import api.endpoints.bfLoss.create.v3.response.CreateBFLossResponse
import api.endpoints.bfLoss.list.v3.response.{ListBFLossesItem, ListBFLossesResponse}
import api.endpoints.bfLoss.retrieve.v3.response.RetrieveBFLossResponse
import api.endpoints.lossClaim.amendOrder.v3.response.AmendLossClaimsOrderResponse
import api.endpoints.lossClaim.amendType.v3.response.AmendLossClaimTypeResponse
import api.endpoints.lossClaim.create.v3.response.CreateLossClaimResponse
import api.endpoints.lossClaim.list.v3.response.{ListLossClaimsItem, ListLossClaimsResponse}
import api.endpoints.lossClaim.retrieve.v3.response.RetrieveLossClaimResponse
import api.models.errors.ErrorWrapper
import api.models.outcomes.ResponseWrapper

package object services {

  type CreateBFLossOutcome = Either[ErrorWrapper, ResponseWrapper[CreateBFLossResponse]]

  type RetrieveBFLossOutcome = Either[ErrorWrapper, ResponseWrapper[RetrieveBFLossResponse]]

  type ListBFLossesOutcome = Either[ErrorWrapper, ResponseWrapper[ListBFLossesResponse[ListBFLossesItem]]]

  type AmendBFLossOutcome = Either[ErrorWrapper, ResponseWrapper[AmendBFLossResponse]]

  type DeleteBFLossOutcome = Either[ErrorWrapper, ResponseWrapper[Unit]]

  type CreateLossClaimOutcome = Either[ErrorWrapper, ResponseWrapper[CreateLossClaimResponse]]

  type RetrieveLossClaimOutcome = Either[ErrorWrapper, ResponseWrapper[RetrieveLossClaimResponse]]

  type ListLossClaimsOutcome = Either[ErrorWrapper, ResponseWrapper[ListLossClaimsResponse[ListLossClaimsItem]]]

  type AmendLossClaimTypeOutcome = Either[ErrorWrapper, ResponseWrapper[AmendLossClaimTypeResponse]]

  type DeleteLossClaimOutcome = Either[ErrorWrapper, ResponseWrapper[Unit]]

  type AmendLossClaimsOrderOutcome = Either[ErrorWrapper, ResponseWrapper[AmendLossClaimsOrderResponse]]

}
