/*
 * Copyright 2023 HM Revenue & Customs
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

package v4.controllers.validators

import shared.controllers.validators.Validator
import shared.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.libs.json.JsValue
import v4.models.request.amendBFLosses.AmendBFLossRequestData

trait MockAmendBFLossValidatorFactory extends TestSuite with MockFactory {

  val mockAmendBFLossValidatorFactory: AmendBFLossValidatorFactory = mock[AmendBFLossValidatorFactory]

  object MockedAmendBFLossValidatorFactory {

    def expectValidator(): CallHandler[Validator[AmendBFLossRequestData]] = {
      (mockAmendBFLossValidatorFactory
        .validator(_: String, _: String, _: JsValue))
        .expects(*, *, *)
    }

  }

  def willUseValidator(use: Validator[AmendBFLossRequestData]): CallHandler[Validator[AmendBFLossRequestData]] = {
    MockedAmendBFLossValidatorFactory
      .expectValidator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: AmendBFLossRequestData): Validator[AmendBFLossRequestData] =
    new Validator[AmendBFLossRequestData] {
      def validate: Validated[Seq[MtdError], AmendBFLossRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[AmendBFLossRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[AmendBFLossRequestData] =
    new Validator[AmendBFLossRequestData] {
      def validate: Validated[Seq[MtdError], AmendBFLossRequestData] = Invalid(result)
    }

}
