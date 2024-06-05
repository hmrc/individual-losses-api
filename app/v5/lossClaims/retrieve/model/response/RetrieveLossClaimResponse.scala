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

package v5.lossClaims.retrieve.model.response

import api.hateoas.{HateoasData, HateoasLinksFactory, Link}
import config.AppConfig
import play.api.libs.json._
import utils.JsonWritesUtil.writesFrom
import v4.models.response.amendBFLosses.AmendBFLossResponse.{amendLossClaimType, deleteLossClaim, getLossClaim}
import v5.lossClaims.retrieve.def1.model.response.Def1_RetrieveLossClaimResponse

trait RetrieveLossClaimResponse

object RetrieveLossClaimResponse {

  implicit val writes: OWrites[RetrieveLossClaimResponse] = writesFrom { case def1: Def1_RetrieveLossClaimResponse =>
    implicitly[OWrites[Def1_RetrieveLossClaimResponse]].writes(def1)
  }

  implicit object GetLinksFactory extends HateoasLinksFactory[RetrieveLossClaimResponse, GetLossClaimHateoasData] {

    override def links(appConfig: AppConfig, data: GetLossClaimHateoasData): Seq[Link] = {
      import data._
      Seq(getLossClaim(appConfig, nino, claimId), deleteLossClaim(appConfig, nino, claimId), amendLossClaimType(appConfig, nino, claimId))
    }

  }

}

case class GetLossClaimHateoasData(nino: String, claimId: String) extends HateoasData
