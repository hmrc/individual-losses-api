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
import v1.models.hateoas.{HateoasData, Link}
import v1.models.outcomes.DesResponse

case class CreateBFLossResponse(id: String)

object CreateBFLossResponse extends Hateoas {
  implicit val writes: OWrites[CreateBFLossResponse] = Json.writes[CreateBFLossResponse]

  implicit val desToMtdReads: Reads[CreateBFLossResponse] =
    (__ \ "lossId").read[String].map(CreateBFLossResponse.apply)

  def links(nino: String, lossId: String): Seq[Link] = List(getBFLoss(nino, lossId), amendBfLoss(nino, lossId), deleteBfLoss(nino, lossId))
}

case class CreateBFLossHateoasData(nino: String, lossId: String, payload: DesResponse[CreateBFLossResponse]) extends HateoasData {
  type A = CreateBFLossResponse
}

object CreateBFLossHateoasData {
  implicit val linkFactory: HateoasLinksFactory[CreateBFLossHateoasData] = new HateoasLinksFactory[CreateBFLossHateoasData] {
    override def links(data: CreateBFLossHateoasData): Seq[Link] = {
      import data._
      CreateBFLossResponse.links(nino, lossId)
    }
  }
}
