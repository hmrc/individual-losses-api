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

package v6.bfLosses.retrieve.def1.model.request

import shared.models.domain.Nino
import v6.bfLosses.common.domain.LossId
import v6.bfLosses.retrieve.RetrieveBFLossSchema
import v6.bfLosses.retrieve.model.request.RetrieveBFLossRequestData

case class Def1_RetrieveBFLossRequestData(nino: Nino, lossId: LossId) extends RetrieveBFLossRequestData {
  override val schema: RetrieveBFLossSchema = RetrieveBFLossSchema.Def1
}
