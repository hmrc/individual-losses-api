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

package v5.bfLosses.list

import api.controllers.validators.Validator
import api.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import v5.bfLosses.list.ListBFLossesValidatorFactory
import v5.bfLosses.list.model.request.ListBFLossesRequestData

trait MockListBFLossesValidatorFactory extends MockFactory {

  val mockListBFLossesValidatorFactory: ListBFLossesValidatorFactory = mock[ListBFLossesValidatorFactory]

  object MockedListBFLossesValidatorFactory {

    def expectValidator(): CallHandler[Validator[ListBFLossesRequestData]] =
      (mockListBFLossesValidatorFactory
        .validator(_: String, _: String, _: Option[String], _: Option[String]))
        .expects(*, *, *, *)

  }

  def willUseValidator(use: Validator[ListBFLossesRequestData]): CallHandler[Validator[ListBFLossesRequestData]] = {
    MockedListBFLossesValidatorFactory
      .expectValidator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: ListBFLossesRequestData): Validator[ListBFLossesRequestData] =
    new Validator[ListBFLossesRequestData] {
      def validate: Validated[Seq[MtdError], ListBFLossesRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[ListBFLossesRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[ListBFLossesRequestData] =
    new Validator[ListBFLossesRequestData] {
      def validate: Validated[Seq[MtdError], ListBFLossesRequestData] = Invalid(result)
    }

}
