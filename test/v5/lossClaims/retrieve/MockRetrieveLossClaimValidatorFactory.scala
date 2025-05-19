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

package v5.lossClaims.retrieve

import shared.controllers.validators.Validator
import shared.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import v5.lossClaims.retrieve.model.request.RetrieveLossClaimRequestData

trait MockRetrieveLossClaimValidatorFactory extends TestSuite with MockFactory {

  val mockRetrieveLossClaimValidatorFactory: RetrieveLossClaimValidatorFactory = mock[RetrieveLossClaimValidatorFactory]

  object MockedRetrieveLossClaimValidatorFactory {

    def expectValidator(): CallHandler[Validator[RetrieveLossClaimRequestData]] =
      (mockRetrieveLossClaimValidatorFactory
        .validator(_: String, _: String))
        .expects(*, *)

  }

  def willUseValidator(use: Validator[RetrieveLossClaimRequestData]): CallHandler[Validator[RetrieveLossClaimRequestData]] =
    MockedRetrieveLossClaimValidatorFactory
      .expectValidator()
      .anyNumberOfTimes()
      .returns(use)

  def returningSuccess(result: RetrieveLossClaimRequestData): Validator[RetrieveLossClaimRequestData] =
    new Validator[RetrieveLossClaimRequestData] {
      def validate: Validated[Seq[MtdError], RetrieveLossClaimRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[RetrieveLossClaimRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[RetrieveLossClaimRequestData] =
    new Validator[RetrieveLossClaimRequestData] {
      def validate: Validated[Seq[MtdError], RetrieveLossClaimRequestData] = Invalid(result)
    }

}
