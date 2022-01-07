/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.controllers.requestParsers

import javax.inject.Inject
import v3.controllers.requestParsers.validators.ListLossClaimsValidator
import v3.models.domain.{Nino, TypeOfClaim, TypeOfLoss}
import v3.models.requestData.{DownstreamTaxYear, ListLossClaimsRawData, ListLossClaimsRequest}

class ListLossClaimsParser @Inject()(val validator: ListLossClaimsValidator)
  extends RequestParser[ListLossClaimsRawData, ListLossClaimsRequest] {

  override protected def requestFor(data: ListLossClaimsRawData): ListLossClaimsRequest = {
    val taxYear = data.taxYearClaimedFor

    val incomeSourceType = for {
      typeOfLossString <- data.typeOfLoss
      typeOfLoss       <- TypeOfLoss.parser.lift(typeOfLossString)
      incomeSourceType <- typeOfLoss.toIncomeSourceType
    } yield incomeSourceType

    ListLossClaimsRequest(
      Nino(data.nino),
      taxYear.map(DownstreamTaxYear.fromMtd),
      incomeSourceType,
      data.businessId,
      data.typeOfClaim.flatMap(TypeOfClaim.parser.lift)
    )
  }
}