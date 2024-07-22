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

package v5.bfLosses.amend.def1.model.response

import api.hateoas.{HateoasLinks, HateoasLinksFactory, Link}
import api.models.domain.{TaxYear, Timestamp}
import config.AppConfig
import play.api.libs.functional.syntax._
import play.api.libs.json._
import v5.bfLosses.amend.model._
import v5.bfLosses.amend.model.response.{AmendBFLossResponse, ResponseData}

case class Def1_AmendBFLossResponse(businessId: String,
                               typeOfLoss: TypeOfLoss,
                               lossAmount: BigDecimal,
                               taxYearBroughtForwardFrom: String,
                               lastModified: Timestamp) extends AmendBFLossResponse

object Def1_AmendBFLossResponse extends HateoasLinks {
  implicit val writes: OWrites[Def1_AmendBFLossResponse] = Json.writes[Def1_AmendBFLossResponse]

  implicit val reads: Reads[Def1_AmendBFLossResponse] = (
    (__ \ "incomeSourceId").read[String] and
      ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
        orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String].map(TaxYear(_)).map(_.asMtd) and
      (__ \ "submissionDate").read[Timestamp]
  )(Def1_AmendBFLossResponse.apply _)

  implicit object LinksFactory extends HateoasLinksFactory[AmendBFLossResponse, ResponseData] {

    override def links(appConfig: AppConfig, data: ResponseData): Seq[Link] = {
      import data._
      Seq(getBFLoss(appConfig, nino, lossId), amendBfLoss(appConfig, nino, lossId), deleteBfLoss(appConfig, nino, lossId))
    }
  }

}
