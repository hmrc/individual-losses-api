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

import v3.controllers.requestParsers.validators.AmendBFLossValidator
import v3.models.domain.Nino
import v3.models.request.amendBFLoss.{AmendBFLossRawData, AmendBFLossRequest, AmendBFLossRequestBody}

import javax.inject.Inject

class AmendBFLossParser @Inject()(val validator: AmendBFLossValidator) extends RequestParser[AmendBFLossRawData, AmendBFLossRequest] {

  override protected def requestFor(data: AmendBFLossRawData): AmendBFLossRequest =
    AmendBFLossRequest(Nino(data.nino), data.lossId, data.body.json.as[AmendBFLossRequestBody])
}
