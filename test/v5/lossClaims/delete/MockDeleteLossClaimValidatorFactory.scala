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

package v5.lossClaims.delete

import shared.controllers.validators.Validator
import shared.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import v5.lossClaims.delete.model.request.DeleteLossClaimRequestData

trait MockDeleteLossClaimValidatorFactory extends TestSuite with MockFactory {

  val mockDeleteLossClaimValidatorFactory: DeleteLossClaimValidatorFactory = mock[DeleteLossClaimValidatorFactory]

  object MockedDeleteLossClaimValidatorFactory {

    def expectValidator(): CallHandler[Validator[DeleteLossClaimRequestData]] =
      (mockDeleteLossClaimValidatorFactory
        .validator(_: String, _: String))
        .expects(*, *)

  }

  def willUseValidator(use: Validator[DeleteLossClaimRequestData]): CallHandler[Validator[DeleteLossClaimRequestData]] = {
    MockedDeleteLossClaimValidatorFactory
      .expectValidator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: DeleteLossClaimRequestData): Validator[DeleteLossClaimRequestData] =
    new Validator[DeleteLossClaimRequestData] {
      def validate: Validated[Seq[MtdError], DeleteLossClaimRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[DeleteLossClaimRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[DeleteLossClaimRequestData] =
    new Validator[DeleteLossClaimRequestData] {
      def validate: Validated[Seq[MtdError], DeleteLossClaimRequestData] = Invalid(result)
    }

}
