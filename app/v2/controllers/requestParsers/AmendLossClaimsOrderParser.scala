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

package v2.controllers.requestParsers

import javax.inject.Inject
import uk.gov.hmrc.domain.Nino
import v2.controllers.requestParsers.validators.AmendLossClaimsOrderValidator
import v2.models.domain.LossClaimsList
import v2.models.requestData.{AmendLossClaimsOrderRawData, AmendLossClaimsOrderRequest, DesTaxYear}

class AmendLossClaimsOrderParser @Inject()(val validator: AmendLossClaimsOrderValidator)
  extends RequestParser[AmendLossClaimsOrderRawData,AmendLossClaimsOrderRequest] {

  override protected def requestFor(data: AmendLossClaimsOrderRawData): AmendLossClaimsOrderRequest = {

    val taxYear: DesTaxYear = data.taxYear match {
      case Some(year) => DesTaxYear.fromMtd(year)
      case None => DesTaxYear.mostRecentTaxYear()
    }

    AmendLossClaimsOrderRequest(Nino(data.nino), taxYear, data.body.json.as[LossClaimsList])
  }



}