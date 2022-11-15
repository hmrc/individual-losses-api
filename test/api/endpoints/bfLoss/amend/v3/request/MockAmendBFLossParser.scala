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

package api.endpoints.bfLoss.amend.v3.request

import api.endpoints.bfLoss.amend.anyVersion.request.AmendBFLossRawData
import api.models.errors.ErrorWrapper
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory

trait MockAmendBFLossParser extends MockFactory {

  implicit val correlationId: String = "X-123"

  val mockAmendBFLossRequestDataParser: AmendBFLossParser = mock[AmendBFLossParser]

  object MockAmendBFLossRequestDataParser {

    def parseRequest(data: AmendBFLossRawData): CallHandler[Either[ErrorWrapper, AmendBFLossRequest]] = {
      (mockAmendBFLossRequestDataParser.parseRequest(_: AmendBFLossRawData)(_: String)).expects(data, *)
    }
  }

}
