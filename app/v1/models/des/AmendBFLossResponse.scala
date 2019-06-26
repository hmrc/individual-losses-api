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

package v1.models.des

import play.api.libs.json._
import play.api.libs.functional.syntax._
import v1.models.domain.BFLoss
import v1.models.requestData.DesTaxYear

case class AmendBFLossResponse(selfEmploymentId: Option[String], typeOfLoss: String, lossAmount: BigDecimal, taxYear: String)

object AmendBFLossResponse {
  implicit val writes: Writes[AmendBFLossResponse] = Json.writes[AmendBFLossResponse]

  implicit val desToMtdReads: Reads[AmendBFLossResponse] = (
    (__ \ "incomeSourceId").readNullable[String] and
      ((__ \ "lossType").read[String].map(BFLoss.convertLossTypeToMtdCode)
        orElse (__ \ "incomeSourceType").read[String].map(BFLoss.convertIncomeSourceTypeToMtdCode)) and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String].map(DesTaxYear.fromDes).map(_.toString)
  )(AmendBFLossResponse.apply _)
}