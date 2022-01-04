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

package v2.models.des

import support.UnitSpec
import utils.enums.EnumJsonSpecSupport
import v2.models.des.ReliefClaimed._
import v2.models.domain.TypeOfClaim

class ReliefClaimedSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[ReliefClaimed](("CF", `CF`), ("CSGI", `CSGI`), ("CFCSGI", `CFCSGI`), ("CSFHL", `CSFHL`))

  "TypeOfClaim" when {
    "getting DES reliefClaimed" must {
      "work" in {
        `CF`.toTypeOfClaim shouldBe TypeOfClaim.`carry-forward`
        `CSGI`.toTypeOfClaim shouldBe TypeOfClaim.`carry-sideways`
        `CFCSGI`.toTypeOfClaim shouldBe TypeOfClaim.`carry-forward-to-carry-sideways`
        `CSFHL`.toTypeOfClaim shouldBe TypeOfClaim.`carry-sideways-fhl`
      }
    }
  }
}
