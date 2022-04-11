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

package v2

import api.endpoints.createBFLoss.v2.model.downstream.CreateBFLossResponse
import api.models.downstream.lossClaim.v2.LossClaimResponse
import api.models.errors.ErrorWrapper
import api.models.outcomes.ResponseWrapper
import v2.models.des._

package object services {

  type CreateBFLossOutcome = Either[ErrorWrapper, ResponseWrapper[CreateBFLossResponse]]

  type RetrieveBFLossOutcome = Either[ErrorWrapper, ResponseWrapper[BFLossResponse]]

  type ListBFLossesOutcome = Either[ErrorWrapper, ResponseWrapper[ListBFLossesResponse[BFLossId]]]

  type DeleteBFLossOutcome = Either[ErrorWrapper, ResponseWrapper[Unit]]

  type CreateLossClaimOutcome = Either[ErrorWrapper, ResponseWrapper[CreateLossClaimResponse]]

  type RetrieveLossClaimOutcome = Either[ErrorWrapper, ResponseWrapper[LossClaimResponse]]

  type ListLossClaimsOutcome = Either[ErrorWrapper, ResponseWrapper[ListLossClaimsResponse[LossClaimId]]]

  type AmendLossClaimOutcome = Either[ErrorWrapper, ResponseWrapper[LossClaimResponse]]

  type DeleteLossClaimOutcome = Either[ErrorWrapper, ResponseWrapper[Unit]]

  type AmendLossClaimsOrderOutcome = Either[ErrorWrapper, ResponseWrapper[AmendLossClaimsOrderResponse]]

}
