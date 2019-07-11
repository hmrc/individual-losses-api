/*
 * Copyright 2019 HM Revenue & Customs
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

import support.UnitSpec
import v1.models.des.ReliefClaimed
import v1.models.domain.TypeOfClaim._

class TypeOfClaimSpec extends UnitSpec {
  "TypeOfClaim" when {
    "getting DES reliefClaimed" must {
      "work" in {
        `carry-forward`.toReliefClaimed shouldBe ReliefClaimed.`CF`
        `carry-sideways`.toReliefClaimed shouldBe ReliefClaimed.`CSGI`
        `carry-forward-to-carry-sideways-general-income`.toReliefClaimed shouldBe ReliefClaimed.`CFCSGI`
        `carry-sideways-fhl`.toReliefClaimed shouldBe ReliefClaimed.`CSFHL`
      }
    }
  }
}
