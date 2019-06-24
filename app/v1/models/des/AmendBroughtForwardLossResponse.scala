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

case class AmendBroughtForwardLossResponse(selfEmploymentId: String, typeOfLoss: String, lossAmount: BigDecimal, taxYear: String)

object AmendBroughtForwardLossResponse {
  implicit val writes: Writes[AmendBroughtForwardLossResponse] = Json.writes[AmendBroughtForwardLossResponse]

  implicit val desToMtdReads: Reads[AmendBroughtForwardLossResponse] = (
    (__ \ "incomeSourceId").read[String] and
      (__ \ "lossType").read[String] and
      (__ \ "broughtForwardLossAmount").read[BigDecimal] and
      (__ \ "taxYear").read[String]
  )(AmendBroughtForwardLossResponse.apply _)
}
