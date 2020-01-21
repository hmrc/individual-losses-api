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

package v1.models.domain

import play.api.libs.json.{Json, Reads, Writes}
import v1.models.requestData.DesTaxYear

case class LossClaim(taxYear: String,
                     typeOfLoss: TypeOfLoss,
                     typeOfClaim: TypeOfClaim,
                     selfEmploymentId: Option[String])
object LossClaim {
  implicit val reads: Reads[LossClaim] = Json.reads[LossClaim]

  implicit val writes: Writes[LossClaim] = (claim: LossClaim) => {

    if (claim.typeOfLoss.isProperty) {
      Json.obj(
        "incomeSourceType" -> claim.typeOfLoss.toIncomeSourceType,
        "reliefClaimed" -> claim.typeOfClaim.toReliefClaimed,
        "taxYear" -> DesTaxYear.fromMtd(claim.taxYear).toString
      )
    }
    else {
      Json.obj(
        "incomeSourceId" -> claim.selfEmploymentId,
        "reliefClaimed" -> claim.typeOfClaim.toReliefClaimed,
        "taxYear" -> DesTaxYear.fromMtd(claim.taxYear).toString
      )
    }
  }
}
