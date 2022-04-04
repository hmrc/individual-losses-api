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

package v2.controllers.requestParsers

import api.controllers.requestParsers.RequestParser
import api.models.domain.Nino
import v2.controllers.requestParsers.validators.AmendLossClaimValidator
import v2.models.domain.AmendLossClaim
import v2.models.requestData.{ AmendLossClaimRawData, AmendLossClaimRequest }

import javax.inject.Inject

class AmendLossClaimParser @Inject()(val validator: AmendLossClaimValidator) extends RequestParser[AmendLossClaimRawData, AmendLossClaimRequest] {

  override protected def requestFor(data: AmendLossClaimRawData): AmendLossClaimRequest =
    AmendLossClaimRequest(Nino(data.nino), data.claimId, data.body.json.as[AmendLossClaim])
}
