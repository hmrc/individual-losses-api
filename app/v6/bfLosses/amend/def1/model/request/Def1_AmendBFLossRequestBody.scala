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

package v6.bfLosses.amend.def1.model.request

import play.api.libs.json._
import v6.bfLosses.amend.model.request.AmendBFLossRequestBody

case class Def1_AmendBFLossRequestBody(lossAmount: BigDecimal) extends AmendBFLossRequestBody

object Def1_AmendBFLossRequestBody {
  implicit val reads: Reads[Def1_AmendBFLossRequestBody] = Json.reads[Def1_AmendBFLossRequestBody]

  implicit val writes: OWrites[Def1_AmendBFLossRequestBody] = (amendBroughtForwardLoss: Def1_AmendBFLossRequestBody) =>
    Json.obj(
      "updatedBroughtForwardLossAmount" -> amendBroughtForwardLoss.lossAmount
    )

}
