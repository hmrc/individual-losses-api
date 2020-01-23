/*
 * Copyright 2020 HM Revenue & Customs
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
import utils.enums.EnumJsonSpecSupport
import v1.models.des.{IncomeSourceType, LossType}
import v1.models.domain.TypeOfLoss._

class TypeOfLossSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[TypeOfLoss](
    ("self-employment", `self-employment`),
    ("self-employment-class4", `self-employment-class4`),
    ("uk-property-non-fhl", `uk-property-non-fhl`),
    ("uk-property-fhl", `uk-property-fhl`)
  )

  "TypeOfLoss" when {
    "getting DES LossType" must {
      "work" in {
        `self-employment`.toLossType shouldBe Some(LossType.INCOME)
        `self-employment-class4`.toLossType shouldBe Some(LossType.CLASS4)
        `uk-property-non-fhl`.toLossType shouldBe None
        `uk-property-fhl`.toLossType shouldBe None
      }
    }

    "getting DES IncomeSourceType" must {
      "work" in {
        `uk-property-non-fhl`.toIncomeSourceType shouldBe Some(IncomeSourceType.`02`)
        `uk-property-fhl`.toIncomeSourceType shouldBe Some(IncomeSourceType.`04`)
        `self-employment`.toIncomeSourceType shouldBe Some(IncomeSourceType.`01`)
        `self-employment-class4`.toIncomeSourceType shouldBe None
      }
    }
  }
}
