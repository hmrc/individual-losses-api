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

package v5.bfLosses.common.domain

import play.api.libs.json.Format
import shared.utils.enums.Enums

enum TypeOfLoss {
  case `uk-property-fhl`, `uk-property`, `foreign-property-fhl-eea`, `foreign-property`, `self-employment`, `self-employment-class4`

  def toIncomeSourceType: Option[IncomeSourceType] = this match {
    case `uk-property-fhl`          => Some(IncomeSourceType.`04`)
    case `uk-property`              => Some(IncomeSourceType.`02`)
    case `foreign-property-fhl-eea` => Some(IncomeSourceType.`03`)
    case `foreign-property`         => Some(IncomeSourceType.`15`)
    case `self-employment`          => Some(IncomeSourceType.`01`)
    case `self-employment-class4`   => None
  }

  def toLossType: Option[LossType] = this match {
    case `self-employment`        => Some(LossType.INCOME)
    case `self-employment-class4` => Some(LossType.CLASS4)
    case _                        => None
  }

}

object TypeOfLoss {
  val parser: PartialFunction[String, TypeOfLoss] = Enums.parser(values)

  given Format[TypeOfLoss] = Enums.format(values)
}
