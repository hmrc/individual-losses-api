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

package v5.bfLosses.retrieve.def1

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.{ResolveNino, Resolver}
import api.models.errors.{LossIdFormatError, MtdError}
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.catsSyntaxTuple2Semigroupal
import v5.bfLosses.common.domain.LossId
import v5.bfLosses.retrieve.def1.model.request.Def1_RetrieveBFLossRequestData
import v5.bfLosses.retrieve.model.request.RetrieveBFLossRequestData

import javax.inject.Singleton
import scala.util.matching.Regex

@Singleton
class Def1_RetrieveBFLossValidator(nino: String, lossId: String) extends Validator[RetrieveBFLossRequestData] {

    def validate: Validated[Seq[MtdError], RetrieveBFLossRequestData] = {
      (
        ResolveNino(nino),
        ResolveBFLossId(lossId)
      ).mapN(Def1_RetrieveBFLossRequestData)
    }

  object ResolveBFLossId extends Resolver[String, LossId] {

    protected val regexFormat: Regex = "^[A-Za-z0-9]{15}$".r
    protected val error: MtdError = LossIdFormatError

    override def apply(value: String, error_NotUsed: Option[MtdError], path: Option[String]): Validated[Seq[MtdError], LossId] =
      if (regexFormat.matches(value)) {
        Valid(LossId(value))
      }
      else {
        Invalid(List(error.maybeWithExtraPath(path)))
      }
  }

}
