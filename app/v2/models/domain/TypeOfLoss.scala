/*
 * Copyright 2021 HM Revenue & Customs
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

package v2.models.domain

import play.api.libs.json.Format
import utils.enums.Enums
import v2.models.des.{IncomeSourceType, LossType}

sealed trait TypeOfLoss {
  def isUkProperty: Boolean                        = false
  def isForeignProperty: Boolean                   = false
  def toLossType: Option[LossType]                 = None
  def toIncomeSourceType: Option[IncomeSourceType] = None
}

object TypeOfLoss {

  case object `uk-property-fhl` extends TypeOfLoss {
    override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`04`)
    override def isUkProperty: Boolean                          = true
  }

  case object `uk-property-non-fhl` extends TypeOfLoss {
    override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`02`)
    override def isUkProperty: Boolean                          = true
  }

  case object `foreign-property-fhl-eea` extends TypeOfLoss {
    override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`03`)
    override def isForeignProperty: Boolean                          = true
  }

  case object `foreign-property` extends TypeOfLoss {
    override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`15`)
    override def isForeignProperty: Boolean                          = true
  }

  case object `self-employment` extends TypeOfLoss {
    override def toLossType: Option[LossType] = Some(LossType.INCOME)
    override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`01`)
  }

  case object `self-employment-class4` extends TypeOfLoss {
    override def toLossType: Option[LossType] = Some(LossType.CLASS4)
  }

  implicit val format: Format[TypeOfLoss] = Enums.format[TypeOfLoss]

  val parser: PartialFunction[String, TypeOfLoss] = Enums.parser[TypeOfLoss]

}
