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

package v5.lossClaims.common.models

import play.api.libs.json.*
import shared.utils.enums.Enums

enum TypeOfLoss(val toIncomeSourceType: Some[IncomeSourceType]) {
  case `uk-property`      extends TypeOfLoss(Some(IncomeSourceType.`02`))
  case `foreign-property` extends TypeOfLoss(Some(IncomeSourceType.`15`))
  case `self-employment`  extends TypeOfLoss(Some(IncomeSourceType.`01`))
}

object TypeOfLoss {
  val parser: PartialFunction[String, TypeOfLoss] = Enums.parser(values)

  given Format[TypeOfLoss] = Enums.format(values)

}
