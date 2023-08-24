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

package v3.services

import api.controllers.RequestContext
import api.models.errors._
import api.services.BaseService
import api.services.v3.Outcomes.RetrieveBFLossOutcome
import cats.implicits._
import v3.connectors.RetrieveBFLossConnector
import v3.models.request.retrieveBFLoss.RetrieveBFLossRequest

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RetrieveBFLossService @Inject() (connector: RetrieveBFLossConnector) extends BaseService {

  def retrieveBFLoss(request: RetrieveBFLossRequest)(implicit ctx: RequestContext, ec: ExecutionContext): Future[RetrieveBFLossOutcome] =
    connector
      .retrieveBFLoss(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  private val errorMap: Map[String, MtdError] = Map(
    "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
    "INVALID_LOSS_ID"           -> LossIdFormatError,
    "NOT_FOUND"                 -> NotFoundError,
    "INVALID_CORRELATIONID"     -> InternalError,
    "SERVER_ERROR"              -> InternalError,
    "SERVICE_UNAVAILABLE"       -> InternalError
  )

}
