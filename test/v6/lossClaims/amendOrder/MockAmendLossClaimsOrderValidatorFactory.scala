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

package v6.lossClaims.amendOrder

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.models.errors.MtdError
import v6.lossClaims.amendOrder.model.request.AmendLossClaimsOrderRequestData

trait MockAmendLossClaimsOrderValidatorFactory extends TestSuite with MockFactory {

  val mockAmendLossClaimsOrderValidatorFactory: AmendLossClaimsOrderValidatorFactory = mock[AmendLossClaimsOrderValidatorFactory]

  object MockedAmendLossClaimsOrderValidatorFactory {

    def expectValidator(): CallHandler[Validator[AmendLossClaimsOrderRequestData]] = {
      (mockAmendLossClaimsOrderValidatorFactory
        .validator(_: String, _: String, _: JsValue))
        .expects(*, *, *)
    }

  }

  def willUseValidator(use: Validator[AmendLossClaimsOrderRequestData]): CallHandler[Validator[AmendLossClaimsOrderRequestData]] = {
    MockedAmendLossClaimsOrderValidatorFactory
      .expectValidator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: AmendLossClaimsOrderRequestData): Validator[AmendLossClaimsOrderRequestData] =
    new Validator[AmendLossClaimsOrderRequestData] {
      def validate: Validated[Seq[MtdError], AmendLossClaimsOrderRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[AmendLossClaimsOrderRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[AmendLossClaimsOrderRequestData] =
    new Validator[AmendLossClaimsOrderRequestData] {
      def validate: Validated[Seq[MtdError], AmendLossClaimsOrderRequestData] = Invalid(result)
    }

}
