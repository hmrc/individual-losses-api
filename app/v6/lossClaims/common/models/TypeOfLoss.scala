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

package v6.lossClaims.common.models

import play.api.libs.json.Format
import shared.utils.enums.Enums

enum TypeOfLoss {
  case `uk-property`, `foreign-property`, `self-employment`

  def toIncomeSourceType: Option[IncomeSourceType] = this match {
    case `uk-property`      => Some(IncomeSourceType.`02`)
    case `foreign-property` => Some(IncomeSourceType.`15`)
    case `self-employment`  => Some(IncomeSourceType.`01`)
  }

}

object TypeOfLoss {
  val parser: PartialFunction[String, TypeOfLoss] = Enums.parser(values)
  given Format[TypeOfLoss]                        = Enums.format(values)
}
