/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.models.des

import play.api.libs.json._
import utils.enums.Enums
import v1.models.domain.TypeOfLoss

sealed trait LossType {
  def toTypeOfLoss: TypeOfLoss
}

object LossType {
  case object INCOME extends LossType {
    override def toTypeOfLoss: TypeOfLoss = TypeOfLoss.`self-employment`
  }
  case object CLASS4 extends LossType {
    override def toTypeOfLoss: TypeOfLoss = TypeOfLoss.`self-employment-class4`
  }

  implicit val format: Format[LossType] = Enums.format[LossType]
}
