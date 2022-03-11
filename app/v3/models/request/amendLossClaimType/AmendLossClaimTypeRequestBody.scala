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

package v3.models.request.amendLossClaimType

import play.api.libs.json.{Json, OWrites, Reads}
import v3.models.domain.lossClaim.TypeOfClaim

case class AmendLossClaimTypeRequestBody(typeOfClaim: TypeOfClaim)

object AmendLossClaimTypeRequestBody {
  implicit val reads: Reads[AmendLossClaimTypeRequestBody] = Json.reads[AmendLossClaimTypeRequestBody]
  implicit val writes: OWrites[AmendLossClaimTypeRequestBody] = (o: AmendLossClaimTypeRequestBody) =>
    Json.obj(
      "updatedReliefClaimedType" -> o.typeOfClaim.toReliefClaimed
  )

}
