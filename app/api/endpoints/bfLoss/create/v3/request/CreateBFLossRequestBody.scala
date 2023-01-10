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

package api.endpoints.bfLoss.create.v3.request

import api.endpoints.bfLoss.domain.v3.TypeOfLoss
import api.endpoints.bfLoss.domain.v3.TypeOfLoss._
import api.models.domain.TaxYear
import play.api.libs.json._

case class CreateBFLossRequestBody(typeOfLoss: TypeOfLoss, businessId: String, taxYearBroughtForwardFrom: String, lossAmount: BigDecimal)

object CreateBFLossRequestBody {
  implicit val reads: Reads[CreateBFLossRequestBody] = Json.reads[CreateBFLossRequestBody]

  implicit val writes: OWrites[CreateBFLossRequestBody] = (loss: CreateBFLossRequestBody) => {
    loss.typeOfLoss match {
      case `uk-property-fhl` | `uk-property-non-fhl` | `foreign-property-fhl-eea` | `foreign-property` =>
        Json.obj(
          "incomeSourceId"            -> loss.businessId,
          "incomeSourceType"          -> loss.typeOfLoss.toIncomeSourceType,
          "taxYearBroughtForwardFrom" -> TaxYear.fromMtd(loss.taxYearBroughtForwardFrom).year,
          "broughtForwardLossAmount"  -> loss.lossAmount
        )
      case TypeOfLoss.`self-employment` | `self-employment-class4` =>
        Json.obj(
          "incomeSourceId"            -> loss.businessId,
          "lossType"                  -> loss.typeOfLoss.toLossType,
          "taxYearBroughtForwardFrom" -> TaxYear.fromMtd(loss.taxYearBroughtForwardFrom).year,
          "broughtForwardLossAmount"  -> loss.lossAmount
        )
    }
  }
}
