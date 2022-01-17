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

package v3.models.downstream

import play.api.libs.json._
import utils.enums.Enums
import v3.models.domain.BFLossTypeOfLoss

sealed trait IncomeSourceType {
  def toTypeOfLoss: BFLossTypeOfLoss
}

object IncomeSourceType {

  case object `01` extends IncomeSourceType {
    override def toTypeOfLoss: BFLossTypeOfLoss = BFLossTypeOfLoss.`self-employment`
  }
  case object `02` extends IncomeSourceType {
    override def toTypeOfLoss: BFLossTypeOfLoss = BFLossTypeOfLoss.`uk-property-non-fhl`
  }
  case object `03` extends IncomeSourceType {
    override def toTypeOfLoss: BFLossTypeOfLoss = BFLossTypeOfLoss.`foreign-property-fhl-eea`
  }
  case object `04` extends IncomeSourceType {
    override def toTypeOfLoss: BFLossTypeOfLoss = BFLossTypeOfLoss.`uk-property-fhl`
  }
  case object `15` extends IncomeSourceType {
    override def toTypeOfLoss: BFLossTypeOfLoss = BFLossTypeOfLoss.`foreign-property`
  }

  implicit val format: Format[IncomeSourceType] = Enums.format[IncomeSourceType]
}
