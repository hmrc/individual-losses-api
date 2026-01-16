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

package v7.bfLosses.retrieve

import shared.controllers.validators.Validator
import v7.bfLosses.retrieve.RetrieveBFLossSchema.Def1
import v7.bfLosses.retrieve.def1.Def1_RetrieveBFLossValidator
import v7.bfLosses.retrieve.model.request.RetrieveBFLossRequestData

class RetrieveBFLossValidatorFactory {

  def validator(nino: String, body: String): Validator[RetrieveBFLossRequestData] = {
    val schema = RetrieveBFLossSchema.schema
    schema match {
      case Def1 => new Def1_RetrieveBFLossValidator(nino, body)
    }
  }

}
