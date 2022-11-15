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

package api.endpoints.bfLoss.delete.v3.request

import api.models.errors.ErrorWrapper
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory

trait MockDeleteBFLossParser extends MockFactory {

  implicit val correlationId: String = "X-123"

  val mockDeleteBFLossParser: DeleteBFLossParser = mock[DeleteBFLossParser]

  object MockDeleteBFLossRequestDataParser {

    def parseRequest(data: DeleteBFLossRawData): CallHandler[Either[ErrorWrapper, DeleteBFLossRequest]] = {
      (mockDeleteBFLossParser.parseRequest(_: DeleteBFLossRawData)(_: String)).expects(data, *)
    }
  }

}
