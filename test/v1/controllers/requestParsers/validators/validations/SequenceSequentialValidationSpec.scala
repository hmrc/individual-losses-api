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

package v1.controllers.requestParsers.validators.validations

import support.UnitSpec
import v1.models.errors.{RuleInvalidSequenceStart, RuleSequenceOrderBroken}

class SequenceSequentialValidationSpec extends UnitSpec {

  "validate" should {
    "return no errors" when {
      "a single number is input" in {
        SequenceSequentialValidation.validate(Seq(1)) shouldBe Nil
      }
      "a valid sorted sequence is input" in {
        SequenceSequentialValidation.validate(Seq(1,2,3,4,5,6,7,8)) shouldBe Nil
      }
      "a valid unsorted sequence is input" in {
        SequenceSequentialValidation.validate(Seq(3,1,5,4,2)) shouldBe Nil
      }
    }
    "return an error" when {
      "no number is input" in {
        SequenceSequentialValidation.validate(Seq()) shouldBe List(RuleInvalidSequenceStart)
      }
      "sequence doesn't have a 1" in {
        SequenceSequentialValidation.validate(Seq(2,3,4,5)) shouldBe List(RuleInvalidSequenceStart)
        SequenceSequentialValidation.validate(Seq(5,2,4,3)) shouldBe List(RuleInvalidSequenceStart)
      }
      "sequence isn't in a sequential order" in {
        SequenceSequentialValidation.validate(Seq(1,3,5,7,9)) shouldBe List(RuleSequenceOrderBroken)
        SequenceSequentialValidation.validate(Seq(5,3,9,1,10)) shouldBe List(RuleSequenceOrderBroken)
      }
    }
    "return multiple errors" when {
      "the sequence doesn't have a 1 and isn't in sequential order" in {
        SequenceSequentialValidation.validate(Seq(7, 20, 12, 9, 3)) shouldBe List(RuleInvalidSequenceStart, RuleSequenceOrderBroken)
      }
    }
  }
}
