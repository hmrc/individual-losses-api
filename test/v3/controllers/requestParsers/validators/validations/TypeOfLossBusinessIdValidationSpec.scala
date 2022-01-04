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

package v3.controllers.requestParsers.validators.validations

import support.UnitSpec
import v3.models.domain.TypeOfLoss
import v3.models.errors.RuleBusinessId

class TypeOfLossBusinessIdValidationSpec extends UnitSpec {
  "validate" should {
    "return no error" when {
      Seq[(TypeOfLoss, Option[String])](
        (TypeOfLoss.`self-employment`, Some("string")),
        (TypeOfLoss.`self-employment-class4`, Some("string")),
        (TypeOfLoss.`foreign-property-fhl-eea`, Some("string")),
        (TypeOfLoss.`foreign-property`, Some("string")),
        (TypeOfLoss.`uk-property-fhl`, None),
        (TypeOfLoss.`uk-property-non-fhl`, None)
      ).foreach {
        case (loss, businessId) => s"passed in $loss and $businessId" in {
          TypeOfLossBusinessIdValidation.validate(loss, businessId) shouldBe NoValidationErrors
        }
      }
    }
    "return a RULE_BUSINESS_ID error" when {
      Seq[(TypeOfLoss, Option[String])](
        (TypeOfLoss.`self-employment`, None),
        (TypeOfLoss.`self-employment-class4`, None),
        (TypeOfLoss.`foreign-property-fhl-eea`, None),
        (TypeOfLoss.`foreign-property`, None),
        (TypeOfLoss.`uk-property-fhl`, Some("string")),
        (TypeOfLoss.`uk-property-non-fhl`, Some("string"))
      ).foreach {
        case (loss, businessId) => s"passed in $loss and $businessId" in {
          TypeOfLossBusinessIdValidation.validate(loss, businessId) shouldBe List(RuleBusinessId)
        }
      }
    }
  }
}
