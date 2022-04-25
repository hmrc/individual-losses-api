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

package api.validations.v3

import support.UnitSpec
import v3.models.errors.{RuleInvalidSequenceStart, RuleSequenceOrderBroken}

class SequenceSequentialValidationSpec extends UnitSpec {

  def sortAndGroupByTwo(seq: Seq[Int]): Seq[Seq[Int]] = seq.sorted.sliding(2).toSeq

  "validate" should {
    "return no errors" when {
      "the sequence contains only 1" in {
        SequenceSequentialValidation.validate(Seq(1)) shouldBe Nil
      }

      "the sequence contains all numbers in range already sorted" in {
        SequenceSequentialValidation.validate(Seq(1, 2, 3, 4, 5)) shouldBe Nil
      }

      "the sequence contains all numbers in range not sorted" in {
        SequenceSequentialValidation.validate(Seq(3, 1, 5, 4, 2)) shouldBe Nil
      }
    }

    "return RuleInvalidSequenceStart" when {
      "the sequence is empty" in {
        SequenceSequentialValidation.validate(Seq()) shouldBe List(RuleInvalidSequenceStart)
      }

      "the sequence doesn't have a 1" in {
        SequenceSequentialValidation.validate(Seq(2, 3, 4, 5)) shouldBe List(RuleInvalidSequenceStart)
        SequenceSequentialValidation.validate(Seq(5, 2, 4, 3)) shouldBe List(RuleInvalidSequenceStart)
      }
    }

    "return RuleSequenceOrderBroken" when {
      "the sequence has a gap" in {
        SequenceSequentialValidation.validate(Seq(1, 3)) shouldBe List(RuleSequenceOrderBroken)
      }

      "the sequence has multiple gaps" in {
        SequenceSequentialValidation.validate(Seq(1, 3, 5, 7, 9)) shouldBe List(RuleSequenceOrderBroken)
      }

      "the sequence has repeated elements" in {
        SequenceSequentialValidation.validate(Seq(1, 2, 2, 3)) shouldBe List(RuleSequenceOrderBroken)
        SequenceSequentialValidation.validate(Seq(1, 1)) shouldBe List(RuleSequenceOrderBroken)
      }
    }

    "return multiple errors" when {
      "the sequence doesn't have a 1 and has gaps" in {
        SequenceSequentialValidation.validate(Seq(2, 3, 5)) shouldBe List(RuleInvalidSequenceStart, RuleSequenceOrderBroken)
      }
    }
  }
}
