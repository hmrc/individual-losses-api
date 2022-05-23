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

package api.endpoints.lossClaim.create.v3.request

import api.models.errors.MtdError
import org.scalamock.handlers.CallHandler1
import org.scalamock.scalatest.MockFactory

class MockCreateLossClaimValidator extends MockFactory {

  val mockValidator: CreateLossClaimValidator = mock[CreateLossClaimValidator]

  object MockValidator {

    def validate(data: CreateLossClaimRawData): CallHandler1[CreateLossClaimRawData, List[MtdError]] = {
      (mockValidator
        .validate(_: CreateLossClaimRawData))
        .expects(data)
    }
  }
}
