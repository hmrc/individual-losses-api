/*
 * Copyright 2020 HM Revenue & Customs
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

case class AmendBFLoss(lossAmount: BigDecimal)

object AmendBFLoss {
  implicit val reads: Reads[AmendBFLoss] = Json.reads[AmendBFLoss]
  implicit val writes: Writes[AmendBFLoss] = (amendBroughtForwardLoss: AmendBFLoss) => Json.obj(
    "updatedBroughtForwardLossAmount" -> amendBroughtForwardLoss.lossAmount
  )
}
