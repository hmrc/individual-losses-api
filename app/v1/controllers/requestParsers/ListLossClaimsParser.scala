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

package v1.controllers.requestParsers

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import v1.controllers.requestParsers.validators.ListLossClaimsValidator
import v1.models.domain.{TypeOfClaim, TypeOfLoss}
import v1.models.requestData.{DesTaxYear, ListLossClaimsRawData, ListLossClaimsRequest}

class ListLossClaimsParser @Inject()(val validator: ListLossClaimsValidator) extends RequestParser[ListLossClaimsRawData, ListLossClaimsRequest] {
  override protected def requestFor(data: ListLossClaimsRawData): ListLossClaimsRequest = {
    val taxYear = data.taxYear

    val incomeSourceType = for {
      typeOfLossString <- data.typeOfLoss
      typeOfLoss       <- TypeOfLoss.parser.lift(typeOfLossString)
      incomeSourceType <- typeOfLoss.toIncomeSourceType
    } yield incomeSourceType

    ListLossClaimsRequest(
      Nino(data.nino),
      taxYear.map(DesTaxYear.fromMtd),
      incomeSourceType,
      data.selfEmploymentId,
      data.claimType.flatMap(TypeOfClaim.parser.lift)    )
  }
}
