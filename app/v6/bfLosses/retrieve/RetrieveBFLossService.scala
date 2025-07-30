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

package v6.bfLosses.retrieve

import cats.implicits.*
import common.errors.LossIdFormatError
import shared.controllers.RequestContext
import shared.models.errors.*
import shared.services.{BaseService, ServiceOutcome}
import v6.bfLosses.retrieve
import v6.bfLosses.retrieve.model.request.RetrieveBFLossRequestData
import v6.bfLosses.retrieve.model.response.RetrieveBFLossResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RetrieveBFLossService @Inject() (connector: retrieve.RetrieveBFLossConnector) extends BaseService {

  def retrieveBFLoss(
      request: RetrieveBFLossRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[RetrieveBFLossResponse]] =
    connector
      .retrieveBFLoss(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  private val errorMap: Map[String, MtdError] = {

    val ifsErrors = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_LOSS_ID"           -> LossIdFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "INVALID_CORRELATIONID"     -> InternalError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

    val hipErrors = Map(
      "1215" -> NinoFormatError,
      "1219" -> LossIdFormatError,
      "5010" -> NotFoundError
    )

    ifsErrors ++ hipErrors
  }

}
