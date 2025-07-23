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

package v4.models.response.retrieveLossClaim

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import shared.config.SharedAppConfig
import shared.hateoas.{HateoasData, HateoasLinksFactory, Link}
import shared.models.domain.{TaxYear, Timestamp}
import v4.V4HateoasLinks
import v4.models.domain.lossClaim.{IncomeSourceType, ReliefClaimed, TypeOfClaim, TypeOfLoss}

case class RetrieveLossClaimResponse(taxYearClaimedFor: String,
                                     typeOfLoss: TypeOfLoss,
                                     typeOfClaim: TypeOfClaim,
                                     businessId: String,
                                     sequence: Option[Int],
                                     lastModified: Timestamp)

object RetrieveLossClaimResponse extends V4HateoasLinks {
  implicit val writes: OWrites[RetrieveLossClaimResponse] = Json.writes[RetrieveLossClaimResponse]

  implicit val reads: Reads[RetrieveLossClaimResponse] = (
    ((JsPath \ "taxYearClaimedFor").read[String].map(taxYear => TaxYear.fromDownstream(taxYear)).map(_.asMtd) orElse
      (JsPath \ "taxYearClaimedFor").read[Int].map(TaxYear.fromDownstreamInt).map(_.asMtd)) and
      ((JsPath \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss) orElse Reads.pure(TypeOfLoss.`self-employment`)) and
      (JsPath \ "reliefClaimed").read[ReliefClaimed].map(_.toTypeOfClaim) and
      (JsPath \ "incomeSourceId").read[String] and
      (JsPath \ "sequence").readNullable[Int] and
      (JsPath \ "submissionDate").read[Timestamp]
  )(RetrieveLossClaimResponse.apply)

  implicit object GetLinksFactory extends HateoasLinksFactory[RetrieveLossClaimResponse, GetLossClaimHateoasData] {

    override def links(appConfig: SharedAppConfig, data: GetLossClaimHateoasData): Seq[Link] = {
      import data.*
      Seq(getLossClaim(appConfig, nino, claimId), deleteLossClaim(appConfig, nino, claimId), amendLossClaimType(appConfig, nino, claimId))
    }

  }

}

case class GetLossClaimHateoasData(nino: String, claimId: String) extends HateoasData
