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

import shared.controllers.validators.Validator
import play.api.libs.json.JsValue
import v6.lossClaims.amendOrder.AmendLossClaimsOrderSchema.Def1
import v6.lossClaims.amendOrder.def1.Def1_AmendLossClaimsOrderValidator
import v6.lossClaims.amendOrder.model.request.AmendLossClaimsOrderRequestData

import javax.inject.Singleton

@Singleton
class AmendLossClaimsOrderValidatorFactory {

  def validator(nino: String, taxYearClaimedFor: String, body: JsValue): Validator[AmendLossClaimsOrderRequestData] = {

    val schema = AmendLossClaimsOrderSchema.schema

    schema match {
      case Def1 => new Def1_AmendLossClaimsOrderValidator(nino, taxYearClaimedFor, body)

    }

  }

}
