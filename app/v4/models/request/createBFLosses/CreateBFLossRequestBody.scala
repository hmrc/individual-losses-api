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

package v4.models.request.createBFLosses

import shared.models.domain.TaxYear
import play.api.libs.json.*
import v4.models.domain.bfLoss.TypeOfLoss
import v4.models.domain.bfLoss.TypeOfLoss.*

case class CreateBFLossRequestBody(typeOfLoss: TypeOfLoss, businessId: String, taxYearBroughtForwardFrom: String, lossAmount: BigDecimal) {
  @transient lazy val taxYear: TaxYear = TaxYear.fromMtd(taxYearBroughtForwardFrom)
}

object CreateBFLossRequestBody {
  implicit val reads: Reads[CreateBFLossRequestBody] = Json.reads[CreateBFLossRequestBody]

  implicit val writes: OWrites[CreateBFLossRequestBody] = (loss: CreateBFLossRequestBody) => {
    loss.typeOfLoss match {
      case `uk-property-fhl` | `uk-property-non-fhl` | `foreign-property-fhl-eea` | `foreign-property` =>
        Json.obj(
          "incomeSourceId"            -> loss.businessId,
          "incomeSourceType"          -> loss.typeOfLoss.toIncomeSourceType,
          "taxYearBroughtForwardFrom" -> loss.taxYear.year,
          "broughtForwardLossAmount"  -> loss.lossAmount
        )
      case `self-employment` | `self-employment-class4` =>
        Json.obj(
          "incomeSourceId"            -> loss.businessId,
          "lossType"                  -> loss.typeOfLoss.toLossType,
          "taxYearBroughtForwardFrom" -> loss.taxYear.year,
          "broughtForwardLossAmount"  -> loss.lossAmount
        )
    }
  }

}
