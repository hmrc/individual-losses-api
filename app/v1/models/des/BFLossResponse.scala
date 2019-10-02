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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import v1.hateoas.{Hateoas, HateoasLinksFactory}
import v1.models.domain.TypeOfLoss
import v1.models.hateoas.{HateoasData, Link}
import v1.models.outcomes.DesResponse
import v1.models.requestData.DesTaxYear

case class BFLossResponse(selfEmploymentId: Option[String], typeOfLoss: TypeOfLoss, lossAmount: BigDecimal, taxYear: String, lastModified: String)

object BFLossResponse extends Hateoas {
  implicit val writes: OWrites[BFLossResponse] = Json.writes[BFLossResponse]

  implicit val desToMtdReads: Reads[BFLossResponse] = (
    (__ \ "incomeSourceId").readNullable[String] and
      ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
        orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String].map(DesTaxYear.fromDes).map(_.toString) and
      (__ \ "submissionDate").read[String]
  )(BFLossResponse.apply _)

  def links(nino: String, lossId: String): Seq[Link] = List(getBFLoss(nino, lossId), amendBfLoss(nino, lossId), deleteBfLoss(nino, lossId))
}

case class AmendBFLossHateoasData(nino: String, lossId: String, payload: DesResponse[BFLossResponse]) extends HateoasData {
  type A = BFLossResponse
}

object AmendBFLossHateoasData {
  implicit val linkFactory: HateoasLinksFactory[AmendBFLossHateoasData] = new HateoasLinksFactory[AmendBFLossHateoasData] {
    override def links(data: AmendBFLossHateoasData): Seq[Link] = {
      import data._
      BFLossResponse.links(nino, lossId)
    }
  }
}

// TODO do these need to be different ^^^ >>>>
case class GetBFLossHateoasData(nino: String, lossId: String, payload: DesResponse[BFLossResponse]) extends HateoasData {
  type A = BFLossResponse
}

object GetBFLossHateoasData {
  implicit val linkFactory: HateoasLinksFactory[GetBFLossHateoasData] = new HateoasLinksFactory[GetBFLossHateoasData] {
    override def links(data: GetBFLossHateoasData): Seq[Link] = {
      import data._
      BFLossResponse.links(nino, lossId)
    }
  }
}
