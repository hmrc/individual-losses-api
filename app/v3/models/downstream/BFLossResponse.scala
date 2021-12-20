/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.libs.json._
import v3.hateoas.{HateoasLinks, HateoasLinksFactory}
import v3.models.domain.TypeOfLoss
import v3.models.hateoas.{HateoasData, Link}
import v3.models.requestData.DownstreamTaxYear

case class BFLossResponse(businessId: String, typeOfLoss: TypeOfLoss, lossAmount: BigDecimal, taxYearBroughtForwardFrom: String, lastModified: String)

object BFLossResponse extends HateoasLinks {
  implicit val writes: OWrites[BFLossResponse] = Json.writes[BFLossResponse]

  implicit val reads: Reads[BFLossResponse] = (
    (__ \ "incomeSourceId").read[String] and
      ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
        orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String].map(DownstreamTaxYear(_)).map(_.toMtd) and
      (__ \ "submissionDate").read[String]
  )(BFLossResponse.apply _)

  implicit object AmendLinksFactory extends HateoasLinksFactory[BFLossResponse, AmendBFLossHateoasData] {
    override def links(appConfig: AppConfig, data: AmendBFLossHateoasData): Seq[Link] = {
      import data._
      Seq(getBFLoss(appConfig, nino, lossId))
    }
  }

  implicit object GetLinksFactory extends HateoasLinksFactory[BFLossResponse, GetBFLossHateoasData] {
    override def links(appConfig: AppConfig, data: GetBFLossHateoasData): Seq[Link] = {
      import data._
      Seq(getBFLoss(appConfig, nino, lossId), deleteBfLoss(appConfig, nino, lossId), amendBfLoss(appConfig, nino, lossId))
    }
  }
}

case class AmendBFLossHateoasData(nino: String, lossId: String) extends HateoasData

case class GetBFLossHateoasData(nino: String, lossId: String) extends HateoasData
