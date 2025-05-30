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

package v4.models.response.amendLossClaimType

import play.api.libs.functional.syntax._
import play.api.libs.json._
import shared.config.SharedAppConfig
import shared.hateoas.{HateoasData, HateoasLinksFactory, Link}
import shared.models.domain.{TaxYear, Timestamp}
import v4.V4HateoasLinks
import v4.models.domain.lossClaim.{IncomeSourceType, ReliefClaimed, TypeOfClaim, TypeOfLoss}

case class AmendLossClaimTypeResponse(taxYearClaimedFor: String,
                                      typeOfLoss: TypeOfLoss,
                                      typeOfClaim: TypeOfClaim,
                                      businessId: String,
                                      sequence: Option[Int],
                                      lastModified: Timestamp)

object AmendLossClaimTypeResponse extends V4HateoasLinks {
  implicit val writes: OWrites[AmendLossClaimTypeResponse] = Json.writes[AmendLossClaimTypeResponse]

  implicit val reads: Reads[AmendLossClaimTypeResponse] = (
    (JsPath \ "taxYearClaimedFor")
      .read[Int]
      .map(_.toString)
      .map(TaxYear(_))
      .map(_.asMtd)
      .orElse((JsPath \ "taxYearClaimedFor").read[String].map(TaxYear(_)).map(_.asMtd)) and
      ((JsPath \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss) orElse Reads.pure(TypeOfLoss.`self-employment`)) and
      (JsPath \ "reliefClaimed").read[ReliefClaimed].map(_.toTypeOfClaim) and
      (JsPath \ "incomeSourceId").read[String] and
      (JsPath \ "sequence").readNullable[Int] and
      (JsPath \ "submissionDate").read[Timestamp]
  )(AmendLossClaimTypeResponse.apply _)

  implicit object AmendLinksFactory extends HateoasLinksFactory[AmendLossClaimTypeResponse, AmendLossClaimTypeHateoasData] {

    override def links(appConfig: SharedAppConfig, data: AmendLossClaimTypeHateoasData): Seq[Link] = {
      import data._
      Seq(getLossClaim(appConfig, nino, claimId), deleteLossClaim(appConfig, nino, claimId), amendLossClaimType(appConfig, nino, claimId))
    }

  }

}

case class AmendLossClaimTypeHateoasData(nino: String, claimId: String) extends HateoasData
