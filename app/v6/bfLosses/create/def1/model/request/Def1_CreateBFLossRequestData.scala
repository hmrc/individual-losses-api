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

package v6.bfLosses.create.def1.model.request

import shared.models.domain.Nino
import v6.bfLosses.create.CreateBFLossSchema
import v6.bfLosses.create.model.request.CreateBFLossRequestData

case class Def1_CreateBFLossRequestData(nino: Nino, broughtForwardLoss: Def1_CreateBFLossRequestBody) extends CreateBFLossRequestData {
  val schema: CreateBFLossSchema = CreateBFLossSchema.Def1
}
