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

import api.controllers.RequestContext
import api.models.errors._
import api.services.{BaseService, ServiceOutcome}
import cats.implicits._
import v4.connectors.CreateLossClaimConnector
import v4.models.request.createLossClaim.CreateLossClaimRequestData
import v4.models.response.createLossClaim.CreateLossClaimResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateLossClaimService @Inject() (connector: CreateLossClaimConnector) extends BaseService {

  def createLossClaim(
      request: CreateLossClaimRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[CreateLossClaimResponse]] =
    connector
      .createLossClaim(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  private val errorMap: PartialFunction[String, MtdError] = {
    case "INVALID_TAXABLE_ENTITY_ID"                                                          => NinoFormatError
    case "DUPLICATE"                                                                          => RuleDuplicateClaimSubmissionError
    case "INCOME_SOURCE_NOT_FOUND"                                                            => NotFoundError
    case "ACCOUNTING_PERIOD_NOT_ENDED"                                                        => RulePeriodNotEnded
    case "INVALID_CLAIM_TYPE"                                                                 => RuleTypeOfClaimInvalid
    case "TAX_YEAR_NOT_SUPPORTED"                                                             => RuleTaxYearNotSupportedError
    case "NO_ACCOUNTING_PERIOD"                                                               => RuleNoAccountingPeriod
    case "INVALID_PAYLOAD" | "SERVER_ERROR" | "SERVICE_UNAVAILABLE" | "INVALID_CORRELATIONID" => InternalError
  }

}
