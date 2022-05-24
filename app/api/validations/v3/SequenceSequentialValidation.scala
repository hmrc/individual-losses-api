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

import api.models.errors.MtdError
import api.models.errors.v3.{ RuleInvalidSequenceStart, RuleSequenceOrderBroken }
import api.validations.NoValidationErrors

object SequenceSequentialValidation {

  def validate(sequences: Seq[Int]): Seq[MtdError] = {
    val sorted = sequences.sorted.toList
    validateStartsWithOne(sorted) ++ validateContinuous(sorted)
  }

  private def validateStartsWithOne(sortedSequence: Seq[Int]): Seq[MtdError] =
    if (sortedSequence.headOption.contains(1)) Nil else List(RuleInvalidSequenceStart)

  private def validateContinuous(sortedSequence: Seq[Int]): Seq[MtdError] = {
    val noGaps = sortedSequence.sliding(2).forall {
      case a :: b :: Nil => b - a == 1
      case _             => true
    }

    if (noGaps) NoValidationErrors else List(RuleSequenceOrderBroken)
  }
}
