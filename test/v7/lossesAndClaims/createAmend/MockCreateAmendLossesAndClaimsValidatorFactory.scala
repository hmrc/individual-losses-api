/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.createAmend

import org.scalamock.handlers.CallHandler
import play.api.libs.json.JsValue
import shared.controllers.validators.{MockValidatorFactory, Validator}
import v7.lossesAndClaims.createAmend.request.CreateAmendLossesAndClaimsRequestData

trait MockCreateAmendLossesAndClaimsValidatorFactory extends MockValidatorFactory[CreateAmendLossesAndClaimsRequestData] {

  val mockCreateAmendLossesAndClaimsValidatorFactory: CreateAmendLossesAndClaimsValidationFactory =
    mock[CreateAmendLossesAndClaimsValidationFactory]

  def validator(): CallHandler[Validator[CreateAmendLossesAndClaimsRequestData]] = {
    (mockCreateAmendLossesAndClaimsValidatorFactory.validator(_: String, _: String, _: String, _: JsValue, _: Boolean)).expects(*, *, *, *, *)
  }

}
