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

package v4.models.response.createBFLosses

import play.api.libs.json.*
import shared.config.SharedAppConfig
import shared.hateoas.{HateoasData, HateoasLinksFactory, Link}
import v4.V4HateoasLinks

case class CreateBFLossResponse(lossId: String)

object CreateBFLossResponse extends V4HateoasLinks {
  implicit val writes: OWrites[CreateBFLossResponse] = Json.writes[CreateBFLossResponse]

  implicit val downstreamToMtdReads: Reads[CreateBFLossResponse] =
    (__ \ "lossId").read[String].map(CreateBFLossResponse.apply)

  implicit object LinksFactory extends HateoasLinksFactory[CreateBFLossResponse, CreateBFLossHateoasData] {

    override def links(appConfig: SharedAppConfig, data: CreateBFLossHateoasData): Seq[Link] = {
      import data.*
      Seq(getBFLoss(appConfig, nino, lossId), deleteBfLoss(appConfig, nino, lossId), amendBfLoss(appConfig, nino, lossId))
    }

  }

}

case class CreateBFLossHateoasData(nino: String, lossId: String) extends HateoasData
