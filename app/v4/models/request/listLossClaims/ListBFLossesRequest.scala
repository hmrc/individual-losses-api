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

package v4.models.request.listLossClaims

import api.models.RawData
import api.models.domain.{Nino, TaxYear}
import v4.models.domain.bfLoss.IncomeSourceType

case class ListBFLossesRawData(
    nino: String,
    taxYearBroughtForwardFrom: String,
    typeOfLoss: Option[String],
    businessId: Option[String]
) extends RawData

case class ListBFLossesRequest(
    nino: Nino,
    taxYearBroughtForwardFrom: TaxYear,
    incomeSourceType: Option[IncomeSourceType],
    businessId: Option[String]
)
