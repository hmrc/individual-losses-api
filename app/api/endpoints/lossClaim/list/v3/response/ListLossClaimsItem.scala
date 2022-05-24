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

package api.endpoints.lossClaim.list.v3.response

import api.endpoints.lossClaim.domain.v3.{IncomeSourceType, ReliefClaimed, TypeOfClaim, TypeOfLoss}
import api.models.domain.DownstreamTaxYear
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ListLossClaimsItem(businessId: String,
                              typeOfClaim: TypeOfClaim,
                              typeOfLoss: TypeOfLoss,
                              taxYearClaimedFor: String,
                              claimId: String,
                              sequence: Option[Int],
                              lastModified: String)

object ListLossClaimsItem {
  implicit val writes: OWrites[ListLossClaimsItem] = Json.writes[ListLossClaimsItem]

  implicit val reads: Reads[ListLossClaimsItem] = (
    (JsPath \ "incomeSourceId").read[String] and
      (JsPath \ "reliefClaimed").read[ReliefClaimed].map(_.toTypeOfClaim) and
      ((JsPath \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss) orElse Reads.pure(TypeOfLoss.`self-employment`)) and
      (JsPath \ "taxYearClaimedFor").read[String].map(DownstreamTaxYear(_)).map(_.toMtd) and
      (JsPath \ "claimId").read[String] and
      (JsPath \ "sequence").readNullable[Int] and
      (JsPath \ "submissionDate").read[String]
  )(ListLossClaimsItem.apply _)
}
