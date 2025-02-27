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

import shared.utils.UnitSpec
import shared.utils.enums.EnumJsonSpecSupport
import v6.lossClaims.common.models.ReliefClaimed._

class ReliefClaimedSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[ReliefClaimed](("CF", `CF`), ("CSGI", `CSGI`), ("CFCSGI", `CFCSGI`), ("CSFHL", `CSFHL`))

  "TypeOfClaim" when {
    "getting downstream reliefClaimed" must {
      "work" in {
        `CF`.toTypeOfClaim shouldBe TypeOfClaim.`carry-forward`
        `CSGI`.toTypeOfClaim shouldBe TypeOfClaim.`carry-sideways`
        `CFCSGI`.toTypeOfClaim shouldBe TypeOfClaim.`carry-forward-to-carry-sideways`
        `CSFHL`.toTypeOfClaim shouldBe TypeOfClaim.`carry-sideways-fhl`
      }
    }
  }

}
