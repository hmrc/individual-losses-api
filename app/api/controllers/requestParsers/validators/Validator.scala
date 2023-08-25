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

package api.controllers.requestParsers.validators

import api.models.RawData
import api.models.errors.MtdError

trait Validator[A <: RawData] {

  type ValidationLevel[T] = T => Seq[MtdError]

  def validate(data: A): Seq[MtdError]

  def run(validationSet: List[A => Seq[Seq[MtdError]]], data: A): Seq[MtdError] = {
    validationSet match {
      case Nil => Nil
      case thisLevel :: remainingLevels =>
        thisLevel(data).flatten match {
          case x if x.isEmpty => run(remainingLevels, data)
          case x              => x
        }
    }
  }

}