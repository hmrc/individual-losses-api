/*
 * Copyright 2020 HM Revenue & Customs
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

package v1

import v1.models.des._
import v1.models.errors.ErrorWrapper
import v1.models.outcomes.DesResponse

package object services {

  type CreateBFLossOutcome = Either[ErrorWrapper, DesResponse[CreateBFLossResponse]]

  type RetrieveBFLossOutcome = Either[ErrorWrapper, DesResponse[BFLossResponse]]

  type ListBFLossesOutcome = Either[ErrorWrapper, DesResponse[ListBFLossesResponse[BFLossId]]]

  type AmendBFLossOutcome = Either[ErrorWrapper, DesResponse[BFLossResponse]]

  type DeleteBFLossOutcome = Either[ErrorWrapper, DesResponse[Unit]]

  type CreateLossClaimOutcome = Either[ErrorWrapper, DesResponse[CreateLossClaimResponse]]

  type RetrieveLossClaimOutcome = Either[ErrorWrapper, DesResponse[LossClaimResponse]]

  type ListLossClaimsOutcome = Either[ErrorWrapper, DesResponse[ListLossClaimsResponse[LossClaimId]]]

  type AmendLossClaimOutcome = Either[ErrorWrapper, DesResponse[LossClaimResponse]]

  type DeleteLossClaimOutcome = Either[ErrorWrapper, DesResponse[Unit]]

  type AmendLossClaimsOrderOutcome = Either[ErrorWrapper, DesResponse[AmendLossClaimsOrderResponse]]


}
