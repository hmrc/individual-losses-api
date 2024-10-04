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

package v5.lossClaims.amendType.def1.model.request

import shared.models.domain.Nino
import v5.lossClaims.amendType.AmendLossClaimTypeSchema
import v5.lossClaims.amendType.model.request.AmendLossClaimTypeRequestData
import v5.lossClaims.common.models.ClaimId

case class Def1_AmendLossClaimTypeRequestData(nino: Nino, claimId: ClaimId, body: Def1_AmendLossClaimTypeRequestBody)
    extends AmendLossClaimTypeRequestData {
  override val schema: AmendLossClaimTypeSchema = AmendLossClaimTypeSchema.Def1
}
