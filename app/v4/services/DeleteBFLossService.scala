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

package v4.services

import cats.implicits._
import common.errors.{LossIdFormatError, RuleDeleteAfterFinalDeclarationError}
import shared.controllers.RequestContext
import shared.models.errors._
import shared.services.{BaseService, ServiceOutcome}
import v4.connectors.DeleteBFLossConnector
import v4.models.request.deleteBFLosses.DeleteBFLossRequestData

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteBFLossService @Inject() (connector: DeleteBFLossConnector) extends BaseService {

  def deleteBFLoss(request: DeleteBFLossRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] =
    connector
      .deleteBFLoss(request)
      .map(_.leftMap(mapDownstreamErrors(commonErrorMap ++ itsaErrorMap ++ itsdErrorMap)))

  private val commonErrorMap: Map[String, MtdError] = Map(
    "SERVER_ERROR"        -> InternalError,
    "SERVICE_UNAVAILABLE" -> InternalError
  )

  private val itsaErrorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_LOSS_ID"           -> LossIdFormatError,
    "NOT_FOUND"                 -> NotFoundError,
    "CONFLICT"                  -> RuleDeleteAfterFinalDeclarationError
  )

  private val itsdErrorMap: Map[String, MtdError] = Map(
    "1215" -> NinoFormatError,
    "1219" -> LossIdFormatError,
    "1227" -> RuleDeleteAfterFinalDeclarationError,
    "5000" -> RuleTaxYearNotSupportedError,
    "5010" -> NotFoundError
  )

}
