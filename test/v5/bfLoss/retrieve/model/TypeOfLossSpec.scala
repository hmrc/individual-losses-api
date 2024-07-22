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

package v5.bfLoss.retrieve.model

import support.UnitSpec
import utils.enums.EnumJsonSpecSupport
import v5.bfLosses.retrieve.model.TypeOfLoss._
import v5.bfLosses.retrieve.model.{IncomeSourceType, LossType, TypeOfLoss}

class TypeOfLossSpec extends UnitSpec with EnumJsonSpecSupport {

  testRoundTrip[TypeOfLoss](
    ("self-employment", `self-employment`),
    ("self-employment-class4", `self-employment-class4`),
    ("uk-property-non-fhl", `uk-property-non-fhl`),
    ("uk-property-fhl", `uk-property-fhl`),
    ("foreign-property-fhl-eea", `foreign-property-fhl-eea`),
    ("foreign-property", `foreign-property`)
  )

  "TypeBFLoss" when {
    "getting LossType" must {
      "work" in {
        `self-employment`.toLossType shouldBe Some(LossType.INCOME)
        `self-employment-class4`.toLossType shouldBe Some(LossType.CLASS4)
        `uk-property-non-fhl`.toLossType shouldBe None
        `uk-property-fhl`.toLossType shouldBe None
        `foreign-property-fhl-eea`.toLossType shouldBe None
        `foreign-property`.toLossType shouldBe None
      }
    }

    "getting IncomeSourceType" must {
      "work" in {
        `uk-property-fhl`.toIncomeSourceType shouldBe Some(IncomeSourceType.`04`)
        `uk-property-non-fhl`.toIncomeSourceType shouldBe Some(IncomeSourceType.`02`)
        `foreign-property-fhl-eea`.toIncomeSourceType shouldBe Some(IncomeSourceType.`03`)
        `foreign-property`.toIncomeSourceType shouldBe Some(IncomeSourceType.`15`)
        `self-employment`.toIncomeSourceType shouldBe Some(IncomeSourceType.`01`)
        `self-employment-class4`.toIncomeSourceType shouldBe None
      }
    }
  }

}
