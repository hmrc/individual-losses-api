/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.createAmend

import cats.implicits.catsSyntaxEither
import common.errors.RuleOutsideAmendmentWindow
import shared.controllers.RequestContext
import shared.models.errors.{
  BusinessIdFormatError,
  InternalError,
  MtdError,
  NinoFormatError,
  NotFoundError,
  RuleCarryBackClaimError,
  RuleCarryForwardAndTerminalLossNotAllowedError,
  RuleMissingPreferenceOrderError,
  RuleTaxYearNotEndedError,
  TaxYearFormatError
}
import shared.services.{BaseService, ServiceOutcome}
import v7.lossesAndClaims.createAmend.request.CreateAmendLossesAndClaimsRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateAmendLossesAndClaimsService @Inject(connector: CreateAmendLossesAndClaimsConnector) extends BaseService {

  def createAmendLossesAndClaims(
      request: CreateAmendLossesAndClaimsRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {
    connector.createAmendLossClaimsAndLosses(request).map(_.leftMap(mapDownstreamErrors(itsdErrorMap)))

  }

  private val itsdErrorMap: Map[String, MtdError] = Map(
    "1215" -> NinoFormatError,
    "1117" -> TaxYearFormatError,
    "1216" -> InternalError,
    "1000" -> InternalError,
    "1007" -> BusinessIdFormatError,
    "1115" -> RuleTaxYearNotEndedError,
    "1253" -> RuleMissingPreferenceOrderError,
    "1254" -> RuleCarryForwardAndTerminalLossNotAllowedError,
    "1262" -> RuleCarryBackClaimError,
    "4200" -> RuleOutsideAmendmentWindow,
    "5000" -> InternalError,
    "5010" -> NotFoundError
  )

}
