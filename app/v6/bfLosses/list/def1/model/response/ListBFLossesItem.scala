/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.bfLosses.list.def1.model.response

import shared.models.domain.TaxYear
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OWrites, Reads, __}
import v6.bfLosses.common.domain.{IncomeSourceType, LossType, TypeOfLoss}

case class ListBFLossesItem(lossId: String,
                            businessId: String,
                            typeOfLoss: TypeOfLoss,
                            lossAmount: BigDecimal,
                            taxYearBroughtForwardFrom: String,
                            lastModified: String)

object ListBFLossesItem {
  implicit val writes: OWrites[ListBFLossesItem] = Json.writes[ListBFLossesItem]

  implicit val reads: Reads[ListBFLossesItem] = (
    (__ \ "lossId").read[String] and
      (__ \ "incomeSourceId").read[String] and
      ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
        orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String].map(TaxYear(_).asMtd) and
      (__ \ "submissionDate").read[String]
  )(ListBFLossesItem.apply _)

}
