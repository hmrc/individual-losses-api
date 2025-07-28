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

package v5.lossClaims.common.models

import play.api.libs.json.Format
import shared.utils.enums.Enums

enum ReliefClaimed(val toTypeOfClaim: TypeOfClaim) {
  case `CF`     extends ReliefClaimed(TypeOfClaim.`carry-forward`)
  case `CSGI`   extends ReliefClaimed(TypeOfClaim.`carry-sideways`)
  case `CFCSGI` extends ReliefClaimed(TypeOfClaim.`carry-forward-to-carry-sideways`)
  case `CSFHL`  extends ReliefClaimed(TypeOfClaim.`carry-sideways-fhl`)
}

object ReliefClaimed {
  given Format[ReliefClaimed] = Enums.format(values)
}
