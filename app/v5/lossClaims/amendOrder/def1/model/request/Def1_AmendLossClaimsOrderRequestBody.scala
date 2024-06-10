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

package v5.lossClaims.amendOrder.def1.model.request

import play.api.libs.json.{Json, OWrites, Reads}
import utils.JsonWritesUtil
import v4.models.domain.lossClaim.TypeOfClaim
import v5.lossClaims.amendOrder.model.request.AmendLossClaimsOrderRequestBody

case class Def1_AmendLossClaimsOrderRequestBody(typeOfClaim: TypeOfClaim, listOfLossClaims: Seq[Claim]) extends AmendLossClaimsOrderRequestBody

object Def1_AmendLossClaimsOrderRequestBody extends JsonWritesUtil{
  implicit val reads: Reads[Def1_AmendLossClaimsOrderRequestBody] = Json.reads

  implicit val writes: OWrites[Def1_AmendLossClaimsOrderRequestBody] = (o: Def1_AmendLossClaimsOrderRequestBody) =>
    Json.obj(
      "claimType"      -> o.typeOfClaim.toReliefClaimed,
      "claimsSequence" -> o.listOfLossClaims
    )

}
