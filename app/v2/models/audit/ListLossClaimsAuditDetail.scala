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

package v2.models.audit

import api.models.audit.AuditResponse
import api.models.auth.UserDetails
import play.api.libs.json.{Json, Writes}

case class ListLossClaimsAuditDetail(userType: String,
                                     agentReferenceNumber: Option[String],
                                     nino: String,
                                     taxYear: Option[String],
                                     typeOfLoss: Option[String],
                                     businessId: Option[String],
                                     claimType: Option[String],
                                     `X-CorrelationId`: String,
                                     response: AuditResponse)

object ListLossClaimsAuditDetail {
  implicit val writes: Writes[ListLossClaimsAuditDetail] = Json.writes[ListLossClaimsAuditDetail]

  def apply(userDetails: UserDetails,
            nino: String,
            taxYear: Option[String],
            typeOfLoss: Option[String],
            businessId: Option[String],
            claimType: Option[String],
            `X-CorrelationId`: String,
            auditResponse: AuditResponse): ListLossClaimsAuditDetail = {

    ListLossClaimsAuditDetail(
      userType = userDetails.userType,
      agentReferenceNumber = userDetails.agentReferenceNumber,
      nino = nino,
      taxYear = taxYear,
      typeOfLoss = typeOfLoss,
      businessId = businessId,
      claimType = claimType,
      `X-CorrelationId`,
      auditResponse
    )
  }
}
