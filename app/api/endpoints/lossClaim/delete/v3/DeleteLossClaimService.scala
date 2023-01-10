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

package api.endpoints.lossClaim.delete.v3

import api.controllers.RequestContext
import api.endpoints.lossClaim.connector.v3.LossClaimConnector
import api.endpoints.lossClaim.delete.v3.request.DeleteLossClaimRequest
import api.models.errors._
import api.services.BaseService
import api.services.v3.Outcomes.DeleteLossClaimOutcome
import cats.implicits._

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class DeleteLossClaimService @Inject() (connector: LossClaimConnector) extends BaseService {

  def deleteLossClaim(request: DeleteLossClaimRequest)(implicit ctx: RequestContext, ec: ExecutionContext): Future[DeleteLossClaimOutcome] =
    connector
      .deleteLossClaim(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  private val errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
    "NOT_FOUND"                 -> NotFoundError,
    "SERVER_ERROR"              -> InternalError,
    "SERVICE_UNAVAILABLE"       -> InternalError
  )

}
