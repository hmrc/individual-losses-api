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

package v6.bfLosses.common.domain

import shared.utils.UnitSpec
import shared.utils.enums.EnumJsonSpecSupport
import v6.bfLosses.common.domain.IncomeSourceType._

class IncomeSourceTypeSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[IncomeSourceType](("01", `01`), ("02", `02`), ("03", `03`), ("04", `04`), ("15", `15`))

  "IncomeSourceType" when {
    "getting downstream IncomeSourceType" must {
      "work" in {
        `01`.toTypeOfLoss shouldBe TypeOfLoss.`self-employment`
        `02`.toTypeOfLoss shouldBe TypeOfLoss.`uk-property`
        `03`.toTypeOfLoss shouldBe TypeOfLoss.`foreign-property-fhl-eea`
        `04`.toTypeOfLoss shouldBe TypeOfLoss.`uk-property-fhl`
        `15`.toTypeOfLoss shouldBe TypeOfLoss.`foreign-property`
      }
    }
  }

}
