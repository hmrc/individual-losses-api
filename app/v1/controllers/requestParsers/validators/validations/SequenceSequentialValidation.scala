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

import v1.models.errors.{MtdError, RuleInvalidSequenceStart, RuleSequenceOrderBroken}

object SequenceSequentialValidation {
  def validate(sequences: Seq[Int]): List[MtdError] = {
    val sortedList = sequences.sorted
    List(
      startsWithOne(sortedList),
      sequenceInOrder(sortedList)
    ).flatten
  }

  private def startsWithOne(sortedSequence: Seq[Int]): List[MtdError] = {
    if(sortedSequence.headOption.getOrElse(0) != 1) {
      List(RuleInvalidSequenceStart)
    } else {
      NoValidationErrors
    }
  }

  def checkIfSequential: PartialFunction[Seq[Int], (Int, Int)] = {
    case a :: b :: Nil if b - a != 1 => (a,b)
  }

  private def sequenceInOrder(sortedSequence: Seq[Int]): List[MtdError] = {
    val inOrder = sortedSequence.sliding(2).collect { checkIfSequential }.isEmpty
    if (inOrder) {
      NoValidationErrors
    } else {
      List(RuleSequenceOrderBroken)
    }
  }
}
