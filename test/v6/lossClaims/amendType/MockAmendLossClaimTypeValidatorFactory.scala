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

package v6.lossClaims.amendType

import shared.controllers.validators.Validator
import shared.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.libs.json.JsValue
import v6.lossClaims.amendType.model.request.AmendLossClaimTypeRequestData

trait MockAmendLossClaimTypeValidatorFactory extends TestSuite with MockFactory {

  val mockAmendLossClaimTypeValidatorFactory: AmendLossClaimTypeValidatorFactory = mock[AmendLossClaimTypeValidatorFactory]

  object MockedAmendLossClaimTypeValidatorFactory {

    def expectValidator(): CallHandler[Validator[AmendLossClaimTypeRequestData]] = {
      (mockAmendLossClaimTypeValidatorFactory
        .validator(_: String, _: String, _: JsValue, _: String))
        .expects(*, *, *, *)
    }

  }

  def willUseValidator(use: Validator[AmendLossClaimTypeRequestData]): CallHandler[Validator[AmendLossClaimTypeRequestData]] = {
    MockedAmendLossClaimTypeValidatorFactory
      .expectValidator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: AmendLossClaimTypeRequestData): Validator[AmendLossClaimTypeRequestData] =
    new Validator[AmendLossClaimTypeRequestData] {
      def validate: Validated[Seq[MtdError], AmendLossClaimTypeRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[AmendLossClaimTypeRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[AmendLossClaimTypeRequestData] =
    new Validator[AmendLossClaimTypeRequestData] {
      def validate: Validated[Seq[MtdError], AmendLossClaimTypeRequestData] = Invalid(result)
    }

}
