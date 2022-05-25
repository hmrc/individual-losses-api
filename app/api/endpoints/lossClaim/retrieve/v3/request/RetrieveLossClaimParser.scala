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

package api.endpoints.lossClaim.retrieve.v3.request

import api.controllers.requestParsers.RequestParser
import api.models.domain.Nino

import javax.inject.Inject

class RetrieveLossClaimParser @Inject()(val validator: RetrieveLossClaimValidator)
    extends RequestParser[RetrieveLossClaimRawData, RetrieveLossClaimRequest] {

  override protected def requestFor(data: RetrieveLossClaimRawData): RetrieveLossClaimRequest =
    RetrieveLossClaimRequest(Nino(data.nino), data.claimId)
}
