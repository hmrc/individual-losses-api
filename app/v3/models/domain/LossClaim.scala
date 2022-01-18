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

package v3.models.domain

import play.api.libs.json._
import v3.models.requestData.DownstreamTaxYear

case class LossClaim(taxYearClaimedFor: String,
                     typeOfLoss: TypeOfClaimLoss,
                     typeOfClaim: TypeOfClaim,
                     businessId: String)

object LossClaim {
  implicit val reads: Reads[LossClaim] = Json.reads[LossClaim]

  implicit val writes: OWrites[LossClaim] = (lossClaim: LossClaim) => {
    lossClaim.typeOfLoss match {
      case TypeOfClaimLoss.`uk-property-non-fhl` =>
        Json.obj(
          "taxYear"   -> DownstreamTaxYear.fromMtd(lossClaim.taxYearClaimedFor).value,
          "incomeSourceType" -> "02",
          "reliefClaimed"    -> lossClaim.typeOfClaim.toReliefClaimed,
          "incomeSourceId"   -> lossClaim.businessId
        )
      case TypeOfClaimLoss.`foreign-property` =>
        Json.obj(
          "taxYear"   -> DownstreamTaxYear.fromMtd(lossClaim.taxYearClaimedFor).value,
          "incomeSourceType" -> "15",
          "reliefClaimed"    -> lossClaim.typeOfClaim.toReliefClaimed,
          "incomeSourceId"   -> lossClaim.businessId
        )
      case _ =>
        // This endpoint only allows for uk-property-non-fhl, foreign-property and self-employment
        // The only remaining option is self-employment, where incomeSourceType isn't sent down
        Json.obj(
          "taxYear" -> DownstreamTaxYear.fromMtd(lossClaim.taxYearClaimedFor).value,
          "reliefClaimed"  -> lossClaim.typeOfClaim.toReliefClaimed,
          "incomeSourceId" -> lossClaim.businessId
        )
    }
  }
}
