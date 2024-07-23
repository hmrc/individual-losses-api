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

package v5.bfLosses.create

import api.controllers.validators.Validator
import api.models.domain.TodaySupplier
import play.api.libs.json.JsValue
import v5.bfLosses.create.CreateBFLossSchema.Def1
import v5.bfLosses.create.def1.Def1_CreateBFLossValidator
import v5.bfLosses.create.model.request.CreateBFLossRequestData

import javax.inject.{Inject, Singleton}

@Singleton
class CreateBFLossValidatorFactory @Inject()(implicit todaySupplier: TodaySupplier = new TodaySupplier) {

  def validator(nino: String, body: JsValue): Validator[CreateBFLossRequestData] = {
    val schema = CreateBFLossSchema.schema
    schema match {
      case Def1 => new Def1_CreateBFLossValidator(nino, body)
    }
  }
}
