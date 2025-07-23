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

package v6.bfLosses.retrieve.def1.model.response

import shared.models.domain.{TaxYear, Timestamp}
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import v6.bfLosses.common.domain.{IncomeSourceType, LossType, TypeOfLoss}
import v6.bfLosses.retrieve.model.response.RetrieveBFLossResponse

case class Def1_RetrieveBFLossResponse(businessId: String,
                                       typeOfLoss: TypeOfLoss,
                                       lossAmount: BigDecimal,
                                       taxYearBroughtForwardFrom: String,
                                       lastModified: Timestamp)
    extends RetrieveBFLossResponse

object Def1_RetrieveBFLossResponse {
  implicit val writes: OWrites[Def1_RetrieveBFLossResponse] = Json.writes[Def1_RetrieveBFLossResponse]

  implicit val reads: Reads[Def1_RetrieveBFLossResponse] = (
    (__ \ "incomeSourceId").read[String] and
      ((__ \ "lossType").read[LossType].map(_.toTypeOfLoss)
        orElse (__ \ "incomeSourceType").read[IncomeSourceType].map(_.toTypeOfLoss)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear")
        .read[String]
        .map(taxYear => TaxYear.fromDownstream(taxYear))
        .map(_.asMtd)
        .orElse((__ \ "taxYearBroughtForwardFrom").read[Int].map(taxYear => TaxYear.fromDownstreamInt(taxYear)).map(_.asMtd)) and
      (__ \ "submissionDate").read[Timestamp]
  )(Def1_RetrieveBFLossResponse.apply _)

}
