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

package v6.lossClaims.amendType.def1.model.request

import play.api.libs.json.{Json, OWrites, Reads}
import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import v6.lossClaims.amendType.model.request.AmendLossClaimTypeRequestBody
import v6.lossClaims.common.models.TypeOfClaim

case class Def1_AmendLossClaimTypeRequestBody(typeOfClaim: TypeOfClaim) extends AmendLossClaimTypeRequestBody

object Def1_AmendLossClaimTypeRequestBody {
  implicit val reads: Reads[Def1_AmendLossClaimTypeRequestBody] = Json.reads

  implicit def writes(implicit appConfig: SharedAppConfig): OWrites[Def1_AmendLossClaimTypeRequestBody] =
    (o: Def1_AmendLossClaimTypeRequestBody) => {
      val updateReliefClaimed =
        if (ConfigFeatureSwitches().isEnabled("ifs_hip_migration_1506")) "updatedReliefClaimed" else "updatedReliefClaimedType"
      Json.obj(
        updateReliefClaimed -> o.typeOfClaim.toReliefClaimed
      )
    }

}
