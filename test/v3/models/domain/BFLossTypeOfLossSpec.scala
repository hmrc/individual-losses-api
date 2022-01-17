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

package v3.models.domain

import support.UnitSpec
import utils.enums.EnumJsonSpecSupport
import v3.models.domain.BFLossTypeOfLoss._
import v3.models.downstream.{IncomeSourceType, LossType}

class BFLossTypeOfLossSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[BFLossTypeOfLoss](
    ("self-employment", `self-employment`),
    ("self-employment-class4", `self-employment-class4`),
    ("uk-property-non-fhl", `uk-property-non-fhl`),
    ("uk-property-fhl", `uk-property-fhl`),
    ("foreign-property-fhl-eea", `foreign-property-fhl-eea`),
    ("foreign-property", `foreign-property`)

  )

  "TypeOfLoss" when {
    "getting downstream LossType" must {
      "work" in {
        `self-employment`.toLossType shouldBe LossType.INCOME
        `self-employment-class4`.toLossType shouldBe LossType.CLASS4
        `uk-property-non-fhl`.toLossType shouldBe None
        `uk-property-fhl`.toLossType shouldBe None
        `foreign-property-fhl-eea`.toLossType shouldBe None
        `foreign-property`.toLossType shouldBe None
      }
    }

    "getting downstream IncomeSourceType" must {
      "work" in {
        `uk-property-fhl`.toIncomeSourceType shouldBe IncomeSourceType.`04`
        `uk-property-non-fhl`.toIncomeSourceType shouldBe IncomeSourceType.`02`
        `foreign-property-fhl-eea`.toIncomeSourceType shouldBe IncomeSourceType.`03`
        `foreign-property`.toIncomeSourceType shouldBe IncomeSourceType.`15`
        `self-employment`.toLossType shouldBe None
        `self-employment-class4`.toLossType shouldBe None
      }
    }
  }
}
