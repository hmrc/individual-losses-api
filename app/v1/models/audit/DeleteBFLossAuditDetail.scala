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

package v1.models.audit

import play.api.libs.json.{Json, Writes}
import v1.models.auth.UserDetails

case class DeleteBFLossAuditDetail(userType: String,
                                   agentReferenceNumber: Option[String],
                                   nino: String,
                                   lossId: String,
                                   `X-CorrelationId`: String,
                                   response: AuditResponse)

object DeleteBFLossAuditDetail {
  implicit val writes: Writes[DeleteBFLossAuditDetail] = Json.writes[DeleteBFLossAuditDetail]

  def apply(userDetails: UserDetails,
            nino: String,
            lossId: String,
            `X-CorrelationId`: String,
            auditResponse: AuditResponse): DeleteBFLossAuditDetail = {

    DeleteBFLossAuditDetail(
      userType = userDetails.userType,
      agentReferenceNumber = userDetails.agentReferenceNumber,
      nino = nino,
      lossId = lossId,
      `X-CorrelationId`,
      auditResponse)
  }
}


