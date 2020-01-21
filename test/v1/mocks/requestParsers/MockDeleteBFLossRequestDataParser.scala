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

package v1.mocks.requestParsers

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import v1.controllers.requestParsers.DeleteBFLossParser
import v1.models.errors.ErrorWrapper
import v1.models.requestData.{DeleteBFLossRawData, DeleteBFLossRequest}

trait MockDeleteBFLossRequestDataParser extends MockFactory {

  val mockDeleteBFLossRequestDataParser: DeleteBFLossParser = mock[DeleteBFLossParser]

  object MockDeleteBFLossRequestDataParser {
    def parseRequest(data: DeleteBFLossRawData): CallHandler[Either[ErrorWrapper, DeleteBFLossRequest]] = {
      (mockDeleteBFLossRequestDataParser.parseRequest(_: DeleteBFLossRawData)).expects(data)
    }
  }

}
