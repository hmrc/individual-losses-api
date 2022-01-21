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

package v3.models.downstream

import support.UnitSpec
import utils.enums.EnumJsonSpecSupport
import v3.models.domain.TypeOfBFLoss
import v3.models.downstream.BFIncomeSourceType._

class BFIncomeSourceTypeSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[BFIncomeSourceType](("01", `01`), ("02", `02`), ("03", `03`), ("04", `04`), ("15", `15`))

  "IncomeSourceType" when {
    "getting downstream IncomeSourceType" must {
      "work" in {
        `01`.toTypeBFLoss shouldBe TypeOfBFLoss.`self-employment`
        `02`.toTypeBFLoss shouldBe TypeOfBFLoss.`uk-property-non-fhl`
        `03`.toTypeBFLoss shouldBe TypeOfBFLoss.`foreign-property-fhl-eea`
        `04`.toTypeBFLoss shouldBe TypeOfBFLoss.`uk-property-fhl`
        `15`.toTypeBFLoss shouldBe TypeOfBFLoss.`foreign-property`
      }
    }
  }
}
