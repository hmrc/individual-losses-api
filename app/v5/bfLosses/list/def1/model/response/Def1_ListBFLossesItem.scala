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

package v5.bfLosses.list.def1.model.response

import api.models.domain.TaxYear
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OWrites, Reads, __}
import v5.bfLosses.list.model.response.ListBFLossesItem
import v5.bfLosses.list.model.{IncomeSourceType, LossType, TypeOfLoss}

case class Def1_ListBFLossesItem(lossId: String,
                            businessId: String,
                            typeOfLoss: TypeOfLoss,
                            lossAmount: BigDecimal,
                            taxYearBroughtForwardFrom: String,
                            lastModified: String) extends ListBFLossesItem

object Def1_ListBFLossesItem {
  implicit val writes: OWrites[Def1_ListBFLossesItem] = Json.writes[Def1_ListBFLossesItem]

  implicit val reads: Reads[Def1_ListBFLossesItem] = (
    (__ \ "lossId").read[String] and
      (__ \ "incomeSourceId").read[String] and
      ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
        orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String].map(TaxYear(_).asMtd) and
      (__ \ "submissionDate").read[String]
  )(Def1_ListBFLossesItem.apply _)

}