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

package api.endpoints.lossClaim.domain.v3

import api.endpoints.lossClaim.domain.v3.TypeOfClaim.{`carry-forward-to-carry-sideways`, `carry-forward`, `carry-sideways-fhl`, `carry-sideways`}
import TypeOfClaim._
import support.UnitSpec
import utils.enums.EnumJsonSpecSupport

class TypeOfClaimSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[TypeOfClaim](
    ("carry-forward", `carry-forward`),
    ("carry-sideways", `carry-sideways`),
    ("carry-forward-to-carry-sideways", `carry-forward-to-carry-sideways`),
    ("carry-sideways-fhl", `carry-sideways-fhl`)
  )

  "TypeOfClaim" when {
    "getting downstream reliefClaimed" must {
      "work" in {
        `carry-forward`.toReliefClaimed shouldBe ReliefClaimed.`CF`
        `carry-sideways`.toReliefClaimed shouldBe ReliefClaimed.`CSGI`
        `carry-forward-to-carry-sideways`.toReliefClaimed shouldBe ReliefClaimed.`CFCSGI`
        `carry-sideways-fhl`.toReliefClaimed shouldBe ReliefClaimed.`CSFHL`
      }
    }
  }
}
