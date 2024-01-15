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

import api.controllers.validators.Validator
import api.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import v4.models.request.deleteBFLosses.DeleteBFLossRequestData

trait MockDeleteBFLossValidatorFactory extends MockFactory {

  val mockDeleteBFLossValidatorFactory: DeleteBFLossValidatorFactory = mock[DeleteBFLossValidatorFactory]

  object MockedDeleteBFLossValidatorFactory {

    def expectValidator(): CallHandler[Validator[DeleteBFLossRequestData]] =
      (mockDeleteBFLossValidatorFactory
        .validator(_: String, _: String))
        .expects(*, *)

  }

  def willUseValidator(use: Validator[DeleteBFLossRequestData]): CallHandler[Validator[DeleteBFLossRequestData]] = {
    MockedDeleteBFLossValidatorFactory
      .expectValidator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: DeleteBFLossRequestData): Validator[DeleteBFLossRequestData] =
    new Validator[DeleteBFLossRequestData] {
      def validate: Validated[Seq[MtdError], DeleteBFLossRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[DeleteBFLossRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[DeleteBFLossRequestData] =
    new Validator[DeleteBFLossRequestData] {
      def validate: Validated[Seq[MtdError], DeleteBFLossRequestData] = Invalid(result)
    }

}
