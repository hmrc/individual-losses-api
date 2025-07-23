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

package v6.bfLosses.common.domain

import play.api.libs.json.Format
import shared.utils.enums.Enums
import v6.bfLosses.common.domain.IncomeSourceType.{`01`, `02`}

sealed trait HasTypeOfLoss {
  def incomeSourceType: Option[IncomeSourceType] = None
  def lossType: Option[LossType]                 = None
}

enum TypeOfLoss(val toIncomeSourceType: Option[IncomeSourceType], val toLossType: Option[LossType]) {
  case `self-employment` extends TypeOfLoss(Some(IncomeSourceType.`01`, Some(LossType.INCOME)))
  case `uk-property`     extends TypeOfLoss(Some(IncomeSourceType.`02`))
}

//case object `uk-property-fhl` extends TypeOfLoss {
//  override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`04`)
//}
//
//case object `uk-property` extends TypeOfLoss {
//  override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`02`)
//}
//
//case object `foreign-property-fhl-eea` extends TypeOfLoss {
//  override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`03`)
//}
//
//case object `foreign-property` extends TypeOfLoss {
//  override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`15`)
//}
//
//case object `self-employment-class4` extends TypeOfLoss {
//  override def toLossType: Option[LossType] = Some(LossType.CLASS4)
//}

object TypeOfLoss {
  val parser: PartialFunction[String, TypeOfLoss] = Enums.parser(values)

  given Format[TypeOfLoss] = Enums.format(values)
}
