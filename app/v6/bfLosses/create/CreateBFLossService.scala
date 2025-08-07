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

package v6.bfLosses.create

import cats.implicits.*
import common.errors._
import shared.controllers.RequestContext
import shared.models.errors.*
import shared.services.{BaseService, ServiceOutcome}
import v6.bfLosses.create.model.request.CreateBFLossRequestData
import v6.bfLosses.create.model.response.CreateBFLossResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreateBFLossService @Inject() (connector: CreateBFLossConnector) extends BaseService {

  def createBFLoss(
      request: CreateBFLossRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[CreateBFLossResponse]] =
    connector
      .createBFLoss(request)
      .map(_.leftMap(mapDownstreamErrors(errorMap)))

  private val errorMap: PartialFunction[String, MtdError] = {
    case "INVALID_TAXABLE_ENTITY_ID"            => NinoFormatError
    case "DUPLICATE_SUBMISSION"                 => RuleDuplicateSubmissionError
    case "TAX_YEAR_NOT_SUPPORTED"               => RuleTaxYearNotSupportedError
    case "INCOME_SOURCE_NOT_FOUND"              => NotFoundError
    case "TAX_YEAR_NOT_ENDED"                   => RuleTaxYearNotEndedError
    case "BFL_NOT_SUPPORTED_FOR_FHL_PROPERTIES" => RuleBflNotSupportedForFhlProperties
    case "OUTSIDE_AMENDMENT_WINDOW"             => RuleOutsideAmendmentWindow
    case "INVALID_TAX_YEAR"                     => TaxYearFormatError
    case "INVALID_CORRELATIONID"                => InternalError
    case "INVALID_PAYLOAD"                      => InternalError
    case "SERVER_ERROR"                         => InternalError
    case "SERVICE_UNAVAILABLE"                  => InternalError
    case "1000"                                 => InternalError
    case "1002"                                 => NotFoundError
    case "1103"                                 => RuleTaxYearNotEndedError
    case "1126"                                 => RuleBflNotSupportedForFhlProperties
    case "1117"                                 => TaxYearFormatError
    case "1215"                                 => NinoFormatError
    case "1216"                                 => InternalError
    case "1226"                                 => RuleDuplicateSubmissionError
    case "4200"                                 => RuleOutsideAmendmentWindow
    case "5000"                                 => RuleTaxYearNotSupportedError
  }

}
