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

package v6.bfLosses.list

import shared.controllers.validators.Validator
import v6.bfLosses.list.ListBFLossesSchema.Def1
import v6.bfLosses.list.def1.Def1_ListBFLossesValidator
import v6.bfLosses.list.model.request.ListBFLossesRequestData

import javax.inject.Singleton

@Singleton
class ListBFLossesValidatorFactory {

  def validator(nino: String,
                taxYearBroughtForwardFrom: String,
                typeOfLoss: Option[String],
                businessId: Option[String]): Validator[ListBFLossesRequestData] = {

    ListBFLossesSchema.schema match {
      case Def1 => new Def1_ListBFLossesValidator(nino, taxYearBroughtForwardFrom, typeOfLoss, businessId)
    }
  }

}
