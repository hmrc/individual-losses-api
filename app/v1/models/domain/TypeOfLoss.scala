/*
 * Copyright 2019 HM Revenue & Customs
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
package v1.models.domain

import play.api.libs.json.{JsString, Reads, Writes}
import v1.models.des.{IncomeSourceType, LossType}

sealed trait TypeOfLoss {
  def name: String = toString
}

trait PropertyLoss {
  def toIncomeSourceType: IncomeSourceType
}

trait IncomeLoss {
  def toLossType: LossType
}

object TypeOfLoss {
  case object `uk-property-fhl` extends TypeOfLoss with PropertyLoss {
    override def toIncomeSourceType: IncomeSourceType = IncomeSourceType.`04`
  }

  case object `uk-property-non-fhl` extends TypeOfLoss with PropertyLoss {
    override def toIncomeSourceType: IncomeSourceType = IncomeSourceType.`02`
  }

  case object `self-employment` extends TypeOfLoss with IncomeLoss {
    override def toLossType: LossType = LossType.INCOME
  }

  case object `self-employment-class4` extends TypeOfLoss with IncomeLoss {
    override def toLossType: LossType = LossType.CLASS4
  }

  case class Other(override val name: String) extends TypeOfLoss

  def parse(name: String): TypeOfLoss = name match {
    case "self-employment"        => `self-employment`
    case "self-employment-class4" => `self-employment-class4`
    case "uk-property-non-fhl"    => `uk-property-non-fhl`
    case "uk-property-fhl"        => `uk-property-fhl`
    case _                        => Other(name)
  }

  implicit val reads: Reads[TypeOfLoss] = implicitly[Reads[String]].map(parse)

  implicit val writes: Writes[TypeOfLoss] = Writes[TypeOfLoss](ts => JsString(ts.name))
}
