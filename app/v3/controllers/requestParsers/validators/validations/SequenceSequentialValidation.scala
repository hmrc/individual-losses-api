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

import api.models.errors.MtdError
import v3.models.errors.{RuleInvalidSequenceStart, RuleSequenceOrderBroken}

object SequenceSequentialValidation {

  def validate(sequences: Seq[Int]): List[MtdError] = {
    val sortedList = sequences.sorted.toList

    validateStartsWithOne(sortedList) ++ validateContinuous(sortedList)
  }

  private def validateStartsWithOne(sortedSequence: Seq[Int]): List[MtdError] =
    if (sortedSequence.headOption.contains(1)) Nil else List(RuleInvalidSequenceStart)

  private def validateContinuous(sortedSequence: List[Int]): List[MtdError] = {
    val noGaps = sortedSequence.sliding(2).forall {
      case a :: b :: Nil => b - a == 1
      case _             => true
    }

    if (noGaps) NoValidationErrors else List(RuleSequenceOrderBroken)
  }
}
