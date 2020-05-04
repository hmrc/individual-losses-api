/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.models.des

import config.AppConfig
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OWrites, Reads, __}
import v1.hateoas.{HateoasLinks, HateoasLinksFactory}
import v1.models.domain.{TypeOfClaim, TypeOfLoss}
import v1.models.hateoas.{HateoasData, Link}
import v1.models.requestData.DesTaxYear

case class LossClaimResponse(selfEmploymentId: Option[String],
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
      (__ \ "taxYearClaimedFor").read[String].map(DesTaxYear.fromDes).map(_.toString) and
      (__ \ "submissionDate").read[String]
    ) (LossClaimResponse.apply _)

  implicit object GetLinksFactory extends HateoasLinksFactory[LossClaimResponse, GetLossClaimHateoasData] {
    override def links(appConfig: AppConfig, data: GetLossClaimHateoasData): Seq[Link] = {
      import data._
      Seq(getLossClaim(appConfig, nino, claimId), deleteLossClaim(appConfig, nino, claimId), amendLossClaim(appConfig, nino, claimId))
    }
  }

  implicit object AmendLinksFactory extends HateoasLinksFactory[LossClaimResponse, AmendLossClaimHateoasData] {
    override def links(appConfig: AppConfig, data: AmendLossClaimHateoasData): Seq[Link] = {
      import data._
      Seq(getLossClaim(appConfig, nino, claimId))
    }
  }
}

case class GetLossClaimHateoasData(nino: String, claimId: String) extends HateoasData

case class AmendLossClaimHateoasData(nino: String, claimId: String) extends HateoasData
