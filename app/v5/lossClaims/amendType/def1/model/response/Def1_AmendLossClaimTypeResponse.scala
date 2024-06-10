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

package v5.lossClaims.amendType.def1.model.response

import api.models.domain.{TaxYear, Timestamp}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import v4.models.domain.lossClaim.{IncomeSourceType, ReliefClaimed, TypeOfClaim, TypeOfLoss}
import v5.lossClaims.amendType.model.response.AmendLossClaimTypeResponse

case class Def1_AmendLossClaimTypeResponse(taxYearClaimedFor: String,
                                           typeOfLoss: TypeOfLoss,
                                           typeOfClaim: TypeOfClaim,
                                           businessId: String,
                                           sequence: Option[Int],
                                           lastModified: Timestamp)
    extends AmendLossClaimTypeResponse

object Def1_AmendLossClaimTypeResponse {
  implicit val writes: OWrites[Def1_AmendLossClaimTypeResponse] = Json.writes[Def1_AmendLossClaimTypeResponse]

  implicit val reads: Reads[Def1_AmendLossClaimTypeResponse] = (
    (JsPath \ "taxYearClaimedFor").read[String].map(TaxYear(_)).map(_.asMtd) and
      ((JsPath \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss) orElse Reads.pure(TypeOfLoss.`self-employment`)) and
      (JsPath \ "reliefClaimed").read[ReliefClaimed].map(_.toTypeOfClaim) and
      (JsPath \ "incomeSourceId").read[String] and
      (JsPath \ "sequence").readNullable[Int] and
      (JsPath \ "submissionDate").read[Timestamp]
  )(Def1_AmendLossClaimTypeResponse.apply _)

}
