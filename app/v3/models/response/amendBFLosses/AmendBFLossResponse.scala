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

package v3.models.response.amendBFLosses

import api.hateoas.{HateoasLinks, HateoasLinksFactory}
import api.models.domain.{TaxYear, Timestamp}
import api.models.hateoas.Link
import config.AppConfig
import play.api.libs.functional.syntax._
import play.api.libs.json._
import v3.models.domain.bfLoss.{IncomeSourceType, LossType, TypeOfLoss}

case class AmendBFLossResponse(businessId: String,
                               typeOfLoss: TypeOfLoss,
                               lossAmount: BigDecimal,
                               taxYearBroughtForwardFrom: String,
                               lastModified: Timestamp)

object AmendBFLossResponse extends HateoasLinks {
  implicit val writes: OWrites[AmendBFLossResponse] = Json.writes[AmendBFLossResponse]

  implicit val reads: Reads[AmendBFLossResponse] = (
    (__ \ "incomeSourceId").read[String] and
      ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
        orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String].map(TaxYear(_)).map(_.asMtd) and
      (__ \ "submissionDate").read[Timestamp]
  )(AmendBFLossResponse.apply _)

  implicit object AmendLinksFactory extends HateoasLinksFactory[AmendBFLossResponse, AmendBFLossHateoasData] {

    override def links(appConfig: AppConfig, data: AmendBFLossHateoasData): Seq[Link] = {
      import data._
      Seq(getBFLoss(appConfig, nino, lossId), amendBfLoss(appConfig, nino, lossId), deleteBfLoss(appConfig, nino, lossId))
    }

  }

}