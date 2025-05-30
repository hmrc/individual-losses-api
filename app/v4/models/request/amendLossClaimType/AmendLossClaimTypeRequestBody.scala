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

package v4.models.request.amendLossClaimType

import play.api.libs.json.{Json, OWrites, Reads}
import shared.config.{ConfigFeatureSwitches, SharedAppConfig}
import v4.models.domain.lossClaim.TypeOfClaim

case class AmendLossClaimTypeRequestBody(typeOfClaim: TypeOfClaim)

object AmendLossClaimTypeRequestBody {
  implicit val reads: Reads[AmendLossClaimTypeRequestBody] = Json.reads[AmendLossClaimTypeRequestBody]

  implicit def writes(implicit appConfig: SharedAppConfig): OWrites[AmendLossClaimTypeRequestBody] =
    (o: AmendLossClaimTypeRequestBody) => {
      val updateReliefClaimed =
        if (ConfigFeatureSwitches().isEnabled("ifs_hip_migration_1506")) "updatedReliefClaimed" else "updatedReliefClaimedType"
      Json.obj(
        updateReliefClaimed -> o.typeOfClaim.toReliefClaimed
      )
    }

}
