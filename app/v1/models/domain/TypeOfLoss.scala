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

import play.api.libs.json.{ JsString, JsonValidationError, Reads, Writes }
import v1.models.des.{ IncomeSourceType, LossType }

sealed trait TypeOfLoss {
  def isProperty: Boolean                          = false
  def toLossType: Option[LossType]                 = None
  def toIncomeSourceType: Option[IncomeSourceType] = None
}

object TypeOfLoss {

  case object `uk-property-fhl` extends TypeOfLoss {
    override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`04`)
    override def isProperty: Boolean                          = true
  }

  case object `uk-property-non-fhl` extends TypeOfLoss {
    override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`02`)
    override def isProperty: Boolean                          = true
  }

  case object `self-employment` extends TypeOfLoss {
    override def toLossType: Option[LossType] = Some(LossType.INCOME)
    override def toIncomeSourceType: Option[IncomeSourceType] = Some(IncomeSourceType.`01`)
  }

  case object `self-employment-class4` extends TypeOfLoss {
    override def toLossType: Option[LossType] = Some(LossType.CLASS4)
  }

  val parser: PartialFunction[String, TypeOfLoss] = {
    case "self-employment"        => `self-employment`
    case "self-employment-class4" => `self-employment-class4`
    case "uk-property-non-fhl"    => `uk-property-non-fhl`
    case "uk-property-fhl"        => `uk-property-fhl`
  }

  implicit val reads: Reads[TypeOfLoss] = implicitly[Reads[String]].collect(JsonValidationError("error.expected.typeOfLoss"))(parser)

  implicit val writes: Writes[TypeOfLoss] = Writes[TypeOfLoss](ts => JsString(ts.toString))
}
