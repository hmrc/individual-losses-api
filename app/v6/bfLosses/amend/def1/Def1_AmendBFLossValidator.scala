/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.bfLosses.amend.def1

import cats.data.Validated
import cats.data.Validated.Invalid
import cats.implicits.catsSyntaxTuple4Semigroupal
import common.errors.LossIdFormatError
import play.api.libs.json.{JsError, JsSuccess, JsValue}
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{
  ResolveNino,
  ResolveNonEmptyJsonObject,
  ResolveParsedNumber,
  ResolveStringPattern,
  ResolveTaxYearMinimum
}
import shared.models.errors.{MtdError, RuleIncorrectOrEmptyBodyError, ValueFormatError}
import v6.bfLosses.amend.def1.model.request.{Def1_AmendBFLossRequestBody, Def1_AmendBFLossRequestData}
import v6.bfLosses.amend.model.request.AmendBFLossRequestData
import v6.bfLosses.common.domain.LossId
import v6.bfLosses.common.minimumTaxYear
import javax.inject.Singleton

@Singleton
class Def1_AmendBFLossValidator(nino: String, lossId: String, taxYear: String, body: JsValue) extends Validator[AmendBFLossRequestData] {
  private val resolveLossId  = new ResolveStringPattern("^[A-Za-z0-9]{15}$".r, LossIdFormatError)
  private val resolveJson    = new ResolveNonEmptyJsonObject[Def1_AmendBFLossRequestBody]()
  private val resolveTaxYear = ResolveTaxYearMinimum(minimumTaxYear)

  def validate: Validated[Seq[MtdError], Def1_AmendBFLossRequestData] = {
    validateJsonFields
      .andThen(_ =>
        (
          ResolveNino(nino),
          resolveLossId(lossId).map(LossId),
          resolveTaxYear(taxYear),
          resolveJson(body)
        ).mapN(Def1_AmendBFLossRequestData))
  }

  private def validateJsonFields: Validated[Seq[MtdError], Unit] = {
    val jsPath = body \ "lossAmount"

    if (jsPath.isDefined) {
      jsPath.validate[BigDecimal] match {
        case JsSuccess(value, _) => ResolveParsedNumber()(value, path = "/lossAmount").map(_ => ())
        case JsError(_)          => Invalid(List(ValueFormatError.withPath("/lossAmount")))
      }
    } else {
      Invalid(List(RuleIncorrectOrEmptyBodyError))
    }
  }

}
