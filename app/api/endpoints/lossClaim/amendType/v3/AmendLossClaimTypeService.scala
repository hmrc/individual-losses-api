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

package api.endpoints.lossClaim.amendType.v3

import api.controllers.RequestContext
import api.endpoints.lossClaim.amendType.v3.request.AmendLossClaimTypeRequest
import api.endpoints.lossClaim.connector.v3.LossClaimConnector
import api.models.errors._
import api.services.v3.Outcomes.AmendLossClaimTypeOutcome
import api.services.{ BaseService, DownstreamResponseMappingSupport }
import cats.implicits._
import utils.Logging

import javax.inject.Inject
import scala.concurrent.{ ExecutionContext, Future }

class AmendLossClaimTypeService @Inject() (connector: LossClaimConnector) extends BaseService with DownstreamResponseMappingSupport with Logging {

  def amendLossClaimType(request: AmendLossClaimTypeRequest)(implicit ctx: RequestContext, ec: ExecutionContext): Future[AmendLossClaimTypeOutcome] =
    connector
      .amendLossClaimType(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  private def errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
    "INVALID_PAYLOAD"           -> InternalError,
    "INVALID_CLAIM_TYPE"        -> RuleTypeOfClaimInvalidForbidden,
    "NOT_FOUND"                 -> NotFoundError,
    "CONFLICT"                  -> RuleClaimTypeNotChanged,
    "INVALID_CORRELATIONID"     -> InternalError,
    "SERVER_ERROR"              -> InternalError,
    "SERVICE_UNAVAILABLE"       -> InternalError
  )

}
