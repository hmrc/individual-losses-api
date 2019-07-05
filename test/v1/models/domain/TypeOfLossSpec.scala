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
import v1.models.des.{ IncomeSourceType, LossType }
import TypeOfLoss._

class TypeOfLossSpec extends UnitSpec {
  "TypeOfLoss" when {
    "getting DES LossType" must {
      "work" in {
        `self-employment`.toLossType shouldBe LossType.INCOME
        `self-employment-class4`.toLossType shouldBe LossType.CLASS4
      }
    }

    "getting DES IncomeSourceType" must {
      "work" in {
        `uk-property-non-fhl`.toIncomeSourceType shouldBe IncomeSourceType.`02`
        `uk-property-fhl`.toIncomeSourceType shouldBe IncomeSourceType.`04`
      }
    }
  }
}
