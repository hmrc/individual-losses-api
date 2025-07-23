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

package v4.models.domain.lossClaim

import play.api.libs.json.*
import shared.utils.enums.Enums

trait HasTypeOfClaim {
  def toReliefClaimed: ReliefClaimed
}

enum TypeOfClaim(val toReliefClaimed: ReliefClaimed) {
  case `carry-forward`                   extends TypeOfClaim(ReliefClaimed.`CF`)
  case `carry-sideways`                  extends TypeOfClaim(ReliefClaimed.`CSGI`)
  case `carry-forward-to-carry-sideways` extends TypeOfClaim(ReliefClaimed.`CFCSGI`)
  case `carry-sideways-fhl`              extends TypeOfClaim(ReliefClaimed.`CSFHL`)
}

object TypeOfClaim {

  given Format[TypeOfClaim] = Enums.format(values)

  val parser: PartialFunction[String, TypeOfClaim] = Enums.parser(values)
}
