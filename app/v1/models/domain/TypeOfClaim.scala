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

package v1.models.domain

import play.api.libs.json.Format
import utils.enums.Enums
import v1.models.des.ReliefClaimed

sealed trait TypeOfClaim {
  def toReliefClaimed: ReliefClaimed
}

object TypeOfClaim {


  case object `carry-forward` extends TypeOfClaim {
    override def toReliefClaimed: ReliefClaimed = ReliefClaimed.`CF`
  }

  case object `carry-sideways` extends TypeOfClaim {
    override def toReliefClaimed: ReliefClaimed = ReliefClaimed.`CSGI`
  }

  case object `carry-forward-to-carry-sideways` extends TypeOfClaim {
    override def toReliefClaimed: ReliefClaimed = ReliefClaimed.`CFCSGI`
  }

  case object `carry-sideways-fhl` extends TypeOfClaim {
    override def toReliefClaimed: ReliefClaimed = ReliefClaimed.`CSFHL`
  }

  implicit val format: Format[TypeOfClaim] = Enums.format[TypeOfClaim]

  val parser: PartialFunction[String, TypeOfClaim] = Enums.parser[TypeOfClaim]
}
