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

package v7.lossesAndClaims.retrieve

import cats.implicits.*
import shared.controllers.RequestContext
import shared.models.errors.*
import shared.services.{BaseService, ServiceOutcome}
import v7.lossesAndClaims.retrieve.model.request.RetrieveLossesAndClaimsRequestData
import v7.lossesAndClaims.retrieve.model.response.RetrieveLossesAndClaimsResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RetrieveLossesAndClaimsService @Inject() (connector: RetrieveLossesAndClaimsConnector) extends BaseService {

  def retrieveLossesAndClaims(request: RetrieveLossesAndClaimsRequestData)(implicit
      ctx: RequestContext,
      ec: ExecutionContext): Future[ServiceOutcome[RetrieveLossesAndClaimsResponse]] = {
    connector
      .retrieveLossesAndClaims(request)
      .map(_.leftMap(mapDownstreamErrors(itsdErrorMap)))
  }

  private val itsdErrorMap: Map[String, MtdError] = Map(
    "1215" -> NinoFormatError,
    "1117" -> TaxYearFormatError,
    "1216" -> InternalError,
    "1007" -> BusinessIdFormatError,
    "5010" -> NotFoundError,
    "5000" -> RuleTaxYearNotSupportedError
  )

}
