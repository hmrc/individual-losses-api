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

package v3.mocks.validators

import api.endpoints.createBFLoss.v3.request.{CreateBFLossRawData, CreateBFLossValidator}
import api.models.errors.MtdError
import org.scalamock.handlers.CallHandler1
import org.scalamock.scalatest.MockFactory

class MockCreateBFLossValidator extends MockFactory {

  val mockValidator: CreateBFLossValidator = mock[CreateBFLossValidator]

  object MockValidator {

    def validate(data: CreateBFLossRawData): CallHandler1[CreateBFLossRawData, List[MtdError]] = {
      (mockValidator
        .validate(_: CreateBFLossRawData))
        .expects(data)
    }
  }
}
