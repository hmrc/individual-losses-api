/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.models.downstream

import config.AppConfig
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OWrites, Reads, __}
import v3.hateoas.{HateoasLinks, HateoasLinksFactory}
import v3.models.domain.{TypeOfClaim, TypeOfLoss}
import v3.models.hateoas.{HateoasData, Link}
import v3.models.requestData.DownstreamTaxYear

case class LossClaimResponse(businessId: Option[String],
                             typeOfLoss: TypeOfLoss,
                             typeOfClaim: TypeOfClaim,
                             taxYear: String,
                             lastModified: String)

object LossClaimResponse extends HateoasLinks {
  implicit val writes: OWrites[LossClaimResponse] = Json.writes[LossClaimResponse]
  implicit val reads: Reads[LossClaimResponse] = (
    (__ \ "incomeSourceId").readNullable[String] and
      ((__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)
        //For SE scenario where incomeSourceType doesn't exist
        orElse Reads.pure(TypeOfLoss.`self-employment`)) and
      (__ \ "reliefClaimed").read[ReliefClaimed].map(_.toTypeOfClaim) and
      (__ \ "taxYearClaimedFor").read[String].map(DownstreamTaxYear(_)).map(_.toMtd) and
      (__ \ "submissionDate").read[String]
    ) (LossClaimResponse.apply _)

  implicit object GetLinksFactory extends HateoasLinksFactory[LossClaimResponse, GetLossClaimHateoasData] {
    override def links(appConfig: AppConfig, data: GetLossClaimHateoasData): Seq[Link] = {
      import data._
      Seq(getLossClaim(appConfig, nino, claimId), deleteLossClaim(appConfig, nino, claimId), amendLossClaimType(appConfig, nino, claimId))
    }
  }

  implicit object AmendLinksFactory extends HateoasLinksFactory[LossClaimResponse, AmendLossClaimTypeHateoasData] {
    override def links(appConfig: AppConfig, data: AmendLossClaimTypeHateoasData): Seq[Link] = {
      import data._
      Seq(getLossClaim(appConfig, nino, claimId))
    }
  }
}

case class GetLossClaimHateoasData(nino: String, claimId: String) extends HateoasData

case class AmendLossClaimTypeHateoasData(nino: String, claimId: String) extends HateoasData
