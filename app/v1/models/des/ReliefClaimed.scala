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
import v1.models.domain.TypeOfClaim

sealed trait ReliefClaimed {
  def toTypeOfClaim: TypeOfClaim
}

object ReliefClaimed {

  case object `CF` extends ReliefClaimed {
    override def toTypeOfClaim: TypeOfClaim = TypeOfClaim.`carry-forward`
  }

  case object `CSGI` extends ReliefClaimed {
    override def toTypeOfClaim: TypeOfClaim = TypeOfClaim.`carry-sideways`
  }

  case object `CFCSGI` extends ReliefClaimed {
    override def toTypeOfClaim: TypeOfClaim = TypeOfClaim.`carry-forward-to-carry-sideways`
  }

  case object `CSFHL` extends ReliefClaimed {
    override def toTypeOfClaim: TypeOfClaim = TypeOfClaim.`carry-sideways-fhl`
  }

  implicit val format: Format[ReliefClaimed] = Enums.format[ReliefClaimed]
}
