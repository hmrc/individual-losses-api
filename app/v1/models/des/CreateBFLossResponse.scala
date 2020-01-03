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
import play.api.libs.json._
import v1.hateoas.{HateoasLinks, HateoasLinksFactory}
import v1.models.hateoas.{HateoasData, Link}

case class CreateBFLossResponse(id: String)

object CreateBFLossResponse extends HateoasLinks {
  implicit val writes: OWrites[CreateBFLossResponse] = Json.writes[CreateBFLossResponse]

  implicit val desToMtdReads: Reads[CreateBFLossResponse] =
    (__ \ "lossId").read[String].map(CreateBFLossResponse.apply)

  implicit object LinksFactory extends HateoasLinksFactory[CreateBFLossResponse, CreateBFLossHateoasData] {
    override def links(appConfig: AppConfig, data: CreateBFLossHateoasData): Seq[Link] = {
      import data._
      Seq(getBFLoss(appConfig, nino, lossId), deleteBfLoss(appConfig, nino, lossId), amendBfLoss(appConfig, nino, lossId))
    }
  }
}

case class CreateBFLossHateoasData(nino: String, lossId: String) extends HateoasData
