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

package v5.bfLosses.create.def1

import api.models.domain.TodaySupplier
import cats.data.Validated
import cats.implicits.catsSyntaxTuple2Semigroupal
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers._
import shared.models.errors.MtdError
import v5.bfLosses.common.resolvers.ResolveBFTypeOfLossFromJson
import v5.bfLosses.create.def1.model.request.{Def1_CreateBFLossRequestBody, Def1_CreateBFLossRequestData}
import v5.bfLosses.create.model.request.CreateBFLossRequestData

import javax.inject.Inject

class Def1_CreateBFLossValidator @Inject() (nino: String, body: JsValue)(implicit todaySupplier: TodaySupplier = new TodaySupplier)
    extends Validator[CreateBFLossRequestData] {
  val minimumTaxYearBFLoss        = 2019
  val minimumTaxYearLossClaim     = 2020
  private val resolveJson         = new ResolveJsonObject[Def1_CreateBFLossRequestBody]()
  private val resolveParsedNumber = ResolveParsedNumber()
  private val resolveTaxYear      = DetailedResolveTaxYear(allowIncompleteTaxYear = false, maybeMinimumTaxYear = Some(minimumTaxYearBFLoss))

  def validate: Validated[Seq[MtdError], CreateBFLossRequestData] =
    ResolveBFTypeOfLossFromJson(body, None, errorPath = Some("/typeOfLoss"))
      .andThen(_ =>
        (
          ResolveNino(nino),
          resolveJson(body)
        ).mapN(Def1_CreateBFLossRequestData)
          .andThen(validateParsedData))

  private def validateParsedData(parsed: Def1_CreateBFLossRequestData): Validated[Seq[MtdError], CreateBFLossRequestData] =
    combine(
      resolveTaxYear(parsed.broughtForwardLoss.taxYearBroughtForwardFrom, None, Some("/taxYearBroughtForwardFrom")),
      ResolveBusinessId(parsed.broughtForwardLoss.businessId),
      resolveParsedNumber(parsed.broughtForwardLoss.lossAmount, path = "/lossAmount")
    ).map(_ => parsed)

}
