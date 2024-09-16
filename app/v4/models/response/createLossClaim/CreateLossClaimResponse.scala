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

package v4.models.response.createLossClaim

import shared.hateoas.{HateoasData, HateoasLinksFactory, Link}
import shared.config.AppConfig
import play.api.libs.json._
import v4.models.response.amendBFLosses.AmendBFLossResponse.{amendLossClaimType, deleteLossClaim, getLossClaim}

case class CreateLossClaimResponse(claimId: String)

object CreateLossClaimResponse {
  implicit val writes: OWrites[CreateLossClaimResponse] = Json.writes[CreateLossClaimResponse]

  implicit val downstreamToMtdReads: Reads[CreateLossClaimResponse] =
    (__ \ "claimId").read[String].map(CreateLossClaimResponse.apply)

  implicit object LinksFactory extends HateoasLinksFactory[CreateLossClaimResponse, CreateLossClaimHateoasData] {

    override def links(appConfig: AppConfig, data: CreateLossClaimHateoasData): Seq[Link] = {
      import data._
      Seq(getLossClaim(appConfig, nino, lossId), deleteLossClaim(appConfig, nino, lossId), amendLossClaimType(appConfig, nino, lossId))
    }

  }

}

case class CreateLossClaimHateoasData(nino: String, lossId: String) extends HateoasData
