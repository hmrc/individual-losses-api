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

package v5.bfLosses.retrieve.def2.model.response

import api.models.domain.{TaxYear, Timestamp}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import v5.bfLosses.common.domain.{IncomeSourceType, LossType, TypeOfLoss}
import v5.bfLosses.retrieve.model.response.RetrieveBFLossResponse

case class Def2_RetrieveBFLossResponse(businessId: String,
                                       typeOfLoss: TypeOfLoss,
                                       lossAmount: BigDecimal,
                                       taxYearBroughtForwardFrom: String,
                                       lastModified: Timestamp)
    extends RetrieveBFLossResponse

object Def2_RetrieveBFLossResponse {

  implicit val writes: OWrites[Def2_RetrieveBFLossResponse] = Json.writes[Def2_RetrieveBFLossResponse].transform { (json: JsValue) =>
    json.as[JsObject] ++ Json.obj("def2Field" -> "discriminator for illustration purposes")
  }

  implicit val reads: Reads[Def2_RetrieveBFLossResponse] = (
    (__ \ "incomeSourceId").read[String] and
      ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
        orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String].map(TaxYear(_)).map(_.asMtd) and
      (__ \ "submissionDate").read[Timestamp]
  )(Def2_RetrieveBFLossResponse.apply _)

}
