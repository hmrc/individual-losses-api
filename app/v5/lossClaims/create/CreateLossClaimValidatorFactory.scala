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

package v5.lossClaims.create

import shared.controllers.validators.Validator
import play.api.libs.json.JsValue
import v5.lossClaims.create.CreateLossClaimSchema.Def1
import v5.lossClaims.create.def1.Def1_CreateLossClaimValidator
import v5.lossClaims.create.model.request.CreateLossClaimRequestData

import javax.inject.Singleton

@Singleton
class CreateLossClaimValidatorFactory {

  def validator(nino: String, body: JsValue): Validator[CreateLossClaimRequestData] = {
    val schema = CreateLossClaimSchema.schema
    schema match {
      case Def1 => new Def1_CreateLossClaimValidator(nino, body)

    }

  }

}
