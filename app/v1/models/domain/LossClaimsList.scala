/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.models.domain

import play.api.libs.json.{JsPath, Reads, Writes}
import play.api.libs.functional.syntax._
import v1.models.des.ReliefClaimed

case class LossClaimsList(claimType: ReliefClaimed, listOfLossClaims: Seq[Claim])

object LossClaimsList {
    implicit val reads: Reads[LossClaimsList] = (
      (JsPath \ "claimType").read[TypeOfClaim].map(_.toReliefClaimed) and
      (JsPath \ "listOfLossClaims").read[Seq[Claim]]
    )(LossClaimsList.apply _)

  implicit val writes: Writes[LossClaimsList] = (
    (JsPath \ "claimType").write[ReliefClaimed] and
      (JsPath \ "claimsSequence").write[Seq[Claim]]
    )(unlift(LossClaimsList.unapply))
}