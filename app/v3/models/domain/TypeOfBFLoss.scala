/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.models.domain

import play.api.libs.json.Format
import utils.enums.Enums
import v3.models.downstream.{BFIncomeSourceType, LossType}

sealed trait TypeOfBFLoss {
  def toIncomeSourceType: Option[BFIncomeSourceType] = None
  def toLossType: Option[LossType]                   = None
}

object TypeOfBFLoss {

  case object `uk-property-fhl` extends TypeOfBFLoss {
    override def toIncomeSourceType: Option[BFIncomeSourceType]      = Some(BFIncomeSourceType.`04`)
  }

  case object `uk-property-non-fhl` extends TypeOfBFLoss {
    override def toIncomeSourceType: Option[BFIncomeSourceType]      = Some(BFIncomeSourceType.`02`)
  }

  case object `foreign-property-fhl-eea` extends TypeOfBFLoss {
    override def toIncomeSourceType: Option[BFIncomeSourceType]      = Some(BFIncomeSourceType.`03`)
  }

  case object `foreign-property` extends TypeOfBFLoss {
    override def toIncomeSourceType: Option[BFIncomeSourceType]      = Some(BFIncomeSourceType.`15`)
  }

  case object `self-employment` extends TypeOfBFLoss {
    override def toLossType: Option[LossType]                       = Some(LossType.INCOME)
  }

  case object `self-employment-class4` extends TypeOfBFLoss {
    override def toLossType: Option[LossType]                       = Some(LossType.CLASS4)
  }

  implicit val format: Format[TypeOfBFLoss] = Enums.format[TypeOfBFLoss]

  val parser: PartialFunction[String, TypeOfBFLoss] = Enums.parser[TypeOfBFLoss]

}
