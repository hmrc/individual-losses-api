/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json._
import v1.hateoas.{Hateoas, HateoasLinksFactory}
import v1.models.hateoas.{HateoasData, HateoasWrapper, Link}

case class BFLossId(id: String)

object BFLossId {
  implicit val writes: OWrites[BFLossId] = Json.writes[BFLossId]

  implicit val reads: Reads[BFLossId] = (JsPath \ "lossId").read[String].map(BFLossId(_))
}

case class ListBFLossesResponse(losses: Seq[BFLossId])

object ListBFLossesResponse extends Hateoas {
  implicit val writes: OWrites[ListBFLossesResponse] =
    Json.writes[ListBFLossesResponse]

  implicit val reads: Reads[ListBFLossesResponse] =
    implicitly[Reads[Seq[BFLossId]]].map(ListBFLossesResponse(_))

  def links(nino: String): Seq[Link] = List(createBfLoss(nino))

  implicit object LinkFactory extends HateoasLinksFactory[ListBFLossesResponse, ListBFLossHateoasData] {
    override def links(data: ListBFLossHateoasData): Seq[Link] = ListBFLossesResponse.links(data.nino)
  }
}

case class ListBFLossesHateoasResponse(losses: Seq[HateoasWrapper[BFLossId]])

object ListBFLossesHateoasResponse {

//  implicit val rds = Reads.seq[HateoasWrapper[BFLossId]]

  implicit val writes: OWrites[ListBFLossesHateoasResponse] =
    Json.writes[ListBFLossesHateoasResponse]

  implicit val reads: Reads[ListBFLossesHateoasResponse] = implicitly[Reads[Seq[HateoasWrapper[BFLossId]]]].map(ListBFLossesHateoasResponse(_))
}

case class ListBFLossHateoasData(nino: String) extends HateoasData
