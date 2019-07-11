/*
 * Copyright 2019 HM Revenue & Customs
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

package v1.models.des

import play.api.libs.functional.syntax._
import play.api.libs.json._
import v1.models.domain.{TypeOfClaim, TypeOfLoss}
import v1.models.requestData.DesTaxYear

case class RetrieveLossClaimResponse(taxYear: String,
                                     typeOfLoss: TypeOfLoss,
                                     selfEmploymentId: Option[String],
                                     typeOfClaim: TypeOfClaim,
                                     lastModified: String
                                    )
object RetrieveLossClaimResponse {

  implicit val writes: OWrites[RetrieveLossClaimResponse] = Json.writes[RetrieveLossClaimResponse]

  implicit val reads: Reads[RetrieveLossClaimResponse] = (
    (__ \ "taxYearClaimedFor").read[String].map(DesTaxYear.fromDes).map(_.toString) and
      ((__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)
        //For SE scenario where incomeSourceType doesn't exist
        orElse Reads.pure(TypeOfLoss.`self-employment`)) and
      (__ \ "incomeSourceId").readNullable[String] and
      (__ \ "reliefClaimed").read[ReliefClaimed].map(_.toTypeOfClaim) and
      (__ \ "submissionDate").read[String]
    )(RetrieveLossClaimResponse.apply _)

}
