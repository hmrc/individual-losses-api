/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.delete

import cats.implicits._
import common.errors.{ClaimIdFormatError, RuleOutsideAmendmentWindow}
import shared.controllers.RequestContext
import shared.models.errors._
import shared.services.{BaseService, ServiceOutcome}
import v6.lossClaims.delete.model.request.DeleteLossClaimRequestData

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteLossClaimService @Inject() (connector: DeleteLossClaimConnector) extends BaseService {

  def deleteLossClaim(request: DeleteLossClaimRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] =
    connector
      .deleteLossClaim(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  private val errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_TAX_YEAR"          -> TaxYearFormatError,
    "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
    "NOT_FOUND"                 -> NotFoundError,
    "OUTSIDE_AMENDMENT_WINDOW"  -> RuleOutsideAmendmentWindow,
    "SERVER_ERROR"              -> InternalError,
    "SERVICE_UNAVAILABLE"       -> InternalError
  )

}
