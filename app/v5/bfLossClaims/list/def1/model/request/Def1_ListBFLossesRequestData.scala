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

package v5.bfLossClaims.list.def1.model.request

import api.models.domain.{BusinessId, Nino, TaxYear}
import v5.bfLossClaims.list.ListBFLossesSchema
import v5.bfLossClaims.list.model.IncomeSourceType
import v5.bfLossClaims.list.model.request.ListBFLossesRequestData

case class Def1_ListBFLossesRequestData(
                                         nino: Nino,
                                         taxYearBroughtForwardFrom: TaxYear,
                                         incomeSourceType: Option[IncomeSourceType],
                                         businessId: Option[BusinessId]) extends ListBFLossesRequestData{
  override val schema: ListBFLossesSchema = ListBFLossesSchema.Def1
}
