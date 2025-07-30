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

package v6.lossClaims.common.models

import play.api.libs.json.Format
import shared.utils.enums.Enums

enum TypeOfClaim {
  case `carry-forward`, `carry-sideways`, `carry-forward-to-carry-sideways`, `carry-sideways-fhl`

  def toReliefClaimed: ReliefClaimed = this match {
    case `carry-forward`                   => ReliefClaimed.`CF`
    case `carry-sideways`                  => ReliefClaimed.`CSGI`
    case `carry-forward-to-carry-sideways` => ReliefClaimed.`CFCSGI`
    case `carry-sideways-fhl`              => ReliefClaimed.`CSFHL`
  }

}

object TypeOfClaim {
  val parser: PartialFunction[String, TypeOfClaim] = Enums.parser(values)
  given Format[TypeOfClaim]                        = Enums.format(values)
}
