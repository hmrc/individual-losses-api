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

import cats.implicits.*
import common.errors.{LossIdFormatError, RuleLossAmountNotChanged}
import shared.controllers.RequestContext
import shared.models.errors.*
import shared.services.{BaseService, ServiceOutcome}
import v4.connectors.AmendBFLossConnector
import v4.models.request.amendBFLosses.AmendBFLossRequestData
import v4.models.response.amendBFLosses.AmendBFLossResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AmendBFLossService @Inject() (connector: AmendBFLossConnector) extends BaseService {

  def amendBFLoss(request: AmendBFLossRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[AmendBFLossResponse]] =
    connector
      .amendBFLoss(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  private val errorMap: Map[String, MtdError] = {

    val ifsErrors = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_LOSS_ID"           -> LossIdFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "CONFLICT"                  -> RuleLossAmountNotChanged,
      "INVALID_CORRELATIONID"     -> InternalError,
      "INVALID_PAYLOAD"           -> InternalError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

    val hipErrors = Map(
      "1000" -> InternalError,
      "1117" -> TaxYearFormatError,
      "1215" -> NinoFormatError,
      "1216" -> InternalError,
      "1219" -> LossIdFormatError,
      "1225" -> RuleLossAmountNotChanged,
      "5000" -> RuleTaxYearNotSupportedError,
      "5010" -> NotFoundError
    )

    ifsErrors ++ hipErrors
  }

}
