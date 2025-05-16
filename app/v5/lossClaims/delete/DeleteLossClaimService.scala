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

package v5.lossClaims.delete

import cats.implicits._
import common.errors.{ClaimIdFormatError, TaxYearClaimedForFormatError}
import shared.controllers.RequestContext
import shared.models.domain.TaxYear
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.{BaseService, ServiceOutcome}
import v5.lossClaims.delete.model.request.DeleteLossClaimRequestData
import v5.lossClaims.retrieve.RetrieveLossClaimConnector
import v5.lossClaims.retrieve.def1.model.request.Def1_RetrieveLossClaimRequestData
import v5.lossClaims.retrieve.def1.model.response.Def1_RetrieveLossClaimResponse
import v5.lossClaims.retrieve.model.response.RetrieveLossClaimResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteLossClaimService @Inject() (retrieveConnector: RetrieveLossClaimConnector, deleteConnector: DeleteLossClaimConnector)
    extends BaseService {

  def deleteLossClaim(request: DeleteLossClaimRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {
    val retrieveRequest: Def1_RetrieveLossClaimRequestData = Def1_RetrieveLossClaimRequestData(request.nino, request.claimId)

    for {
      retrieveResult <- retrieveConnector.retrieveLossClaim(retrieveRequest)
      deleteResult <- retrieveResult match {
        case Right(ResponseWrapper(_, response: RetrieveLossClaimResponse)) =>
          val taxYear: TaxYear = TaxYear.fromMtd(response.asInstanceOf[Def1_RetrieveLossClaimResponse].taxYearClaimedFor)
          deleteConnector.deleteLossClaim(request, taxYear).map(_.leftMap(mapDownstreamErrors(errorMap)))

        case Left(error) => Future.successful(Left(mapDownstreamErrors(errorMap)(error)))
      }
    } yield deleteResult
  }

  private val errorMap: Map[String, MtdError] = {
    val itsaErrors = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_CLAIM_ID"          -> ClaimIdFormatError,
      "NOT_FOUND"                 -> NotFoundError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

    val itsdErrors = List(
      "1117" -> TaxYearClaimedForFormatError,
      "1215" -> NinoFormatError,
      "1216" -> InternalError,
      "1220" -> ClaimIdFormatError,
      "5000" -> RuleTaxYearNotSupportedError,
      "5010" -> NotFoundError
    )

    itsaErrors ++ itsdErrors

  }

}
