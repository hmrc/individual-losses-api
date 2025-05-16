/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.list

import shared.controllers.validators.Validator
import shared.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import v6.lossClaims.list.model.request.ListLossClaimsRequestData

trait MockListLossClaimsValidatorFactory extends TestSuite with MockFactory {

  val mockListLossClaimsValidatorFactory: ListLossClaimsValidatorFactory = mock[ListLossClaimsValidatorFactory]

  object MockedListLossClaimsValidatorFactory {

    def expectValidator(): CallHandler[Validator[ListLossClaimsRequestData]] =
      (mockListLossClaimsValidatorFactory
        .validator(_: String, _: String, _: Option[String], _: Option[String], _: Option[String]))
        .expects(*, *, *, *, *)

  }

  def willUseValidator(use: Validator[ListLossClaimsRequestData]): CallHandler[Validator[ListLossClaimsRequestData]] =
    MockedListLossClaimsValidatorFactory
      .expectValidator()
      .anyNumberOfTimes()
      .returns(use)

  def returningSuccess(result: ListLossClaimsRequestData): Validator[ListLossClaimsRequestData] =
    new Validator[ListLossClaimsRequestData] {
      def validate: Validated[Seq[MtdError], ListLossClaimsRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[ListLossClaimsRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[ListLossClaimsRequestData] =
    new Validator[ListLossClaimsRequestData] {
      def validate: Validated[Seq[MtdError], ListLossClaimsRequestData] = Invalid(result)
    }

}
