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

package v3.models.domain

import play.api.libs.json._
import v3.models.requestData.DownstreamTaxYear

case class BFLoss(typeOfLoss: TypeOfLoss,
                  businessId: Option[String],
                  taxYear: String,
                  lossAmount: BigDecimal)

object BFLoss {
  implicit val reads: Reads[BFLoss] = Json.reads[BFLoss]

  implicit val writes: OWrites[BFLoss] = (loss: BFLoss) => {
    (loss.typeOfLoss.isUkProperty,loss.typeOfLoss.isForeignProperty) match {
      case (true,_) => Json.obj(
        "incomeSourceType" -> loss.typeOfLoss.toIncomeSourceType,
        "taxYear" -> DownstreamTaxYear.fromMtd(loss.taxYear).toString,
        "broughtForwardLossAmount" -> loss.lossAmount
      )
      case (_,true) => Json.obj(
        "incomeSourceId" -> loss.businessId,
        "incomeSourceType" -> loss.typeOfLoss.toIncomeSourceType,
        "taxYear" -> DownstreamTaxYear.fromMtd(loss.taxYear).toString,
        "broughtForwardLossAmount" -> loss.lossAmount
      )
      case (_,_) => Json.obj(
        "incomeSourceId" -> loss.businessId,
        "lossType" -> loss.typeOfLoss.toLossType,
        "taxYear" -> DownstreamTaxYear.fromMtd(loss.taxYear).toString,
        "broughtForwardLossAmount" -> loss.lossAmount
      )
    }
  }
}
