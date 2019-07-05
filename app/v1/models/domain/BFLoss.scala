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

package v1.models.domain

import play.api.libs.json._
import v1.models.requestData.DesTaxYear

case class BFLoss(typeOfLoss: TypeOfLoss, selfEmploymentId: Option[String], taxYear: String, lossAmount: BigDecimal)

object BFLoss {
  implicit val reads: Reads[BFLoss] = Json.reads[BFLoss]

  implicit val writes: Writes[BFLoss] = new Writes[BFLoss] {
    override def writes(loss: BFLoss): JsValue = {

      loss.typeOfLoss match {
        case propertyLoss: PropertyLoss =>
          Json.obj(
            "incomeSourceType"         -> propertyLoss.toIncomeSourceType,
            "taxYear"                  -> DesTaxYear.fromMtd(loss.taxYear).toString,
            "broughtForwardLossAmount" -> loss.lossAmount
          )

        case incomeLoss: IncomeLoss =>
          Json.obj(
            "incomeSourceId"           -> loss.selfEmploymentId,
            "lossType"                 -> incomeLoss.toLossType,
            "taxYear"                  -> DesTaxYear.fromMtd(loss.taxYear).toString,
            "broughtForwardLossAmount" -> loss.lossAmount
          )
      }
    }
  }
}
