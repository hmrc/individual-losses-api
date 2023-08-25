/*
 * Copyright 2023 HM Revenue & Customs
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

package v3.models.domain.lossClaim

import support.UnitSpec
import utils.enums.EnumJsonSpecSupport
import v3.models.domain.lossClaim.TypeOfLoss._

class TypeOfLossSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[TypeOfLoss](
    ("self-employment", `self-employment`),
    ("uk-property-non-fhl", `uk-property-non-fhl`),
    ("foreign-property", `foreign-property`)
  )

  "TypeClaimLoss" when {
    "getting downstream IncomeSourceType" must {
      "work" in {
        `uk-property-non-fhl`.toIncomeSourceType shouldBe Some(IncomeSourceType.`02`)
        `self-employment`.toIncomeSourceType shouldBe Some(IncomeSourceType.`01`)
        `foreign-property`.toIncomeSourceType shouldBe Some(IncomeSourceType.`15`)
      }
    }
  }

}
