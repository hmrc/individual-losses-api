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

import cats.data.Validated
import cats.implicits.catsSyntaxTuple2Semigroupal
import common.errors.TypeOfLossFormatError
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers._
import shared.models.domain.TaxYear
import shared.models.errors._
import v5.bfLosses.common.resolvers.ResolveBFTypeOfLossFromJson
import v5.bfLosses.create.def1.model.request.{Def1_CreateBFLossRequestBody, Def1_CreateBFLossRequestData}
import v5.bfLosses.create.model.request.CreateBFLossRequestData

import java.time.Clock
import javax.inject.Inject

class Def1_CreateBFLossValidator @Inject() (nino: String, body: JsValue)(implicit val clock: Clock = Clock.systemUTC())
    extends Validator[CreateBFLossRequestData] {

  private val minimumTaxYearBFLoss = TaxYear.ending(2019)

  private val resolveJson         = new ResolveJsonObject[Def1_CreateBFLossRequestBody]()
  private val resolveParsedNumber = ResolveParsedNumber()

  def validate: Validated[Seq[MtdError], CreateBFLossRequestData] =
    ResolveBFTypeOfLossFromJson(body, Some(TypeOfLossFormatError.withPath("/typeOfLoss")))
      .andThen(_ =>
        (
          ResolveNino(nino),
          resolveJson(body)
        ).mapN(Def1_CreateBFLossRequestData)
          .andThen(validateParsedData))

  private def validateParsedData(parsed: Def1_CreateBFLossRequestData): Validated[Seq[MtdError], CreateBFLossRequestData] = {
    import parsed.broughtForwardLoss._
    val taxYearErrorPath = "/taxYearBroughtForwardFrom"

    val resolvedTaxYear =
      ResolveTaxYearMinimum(
        minimumTaxYearBFLoss,
        notSupportedError = RuleTaxYearNotSupportedError.withPath(taxYearErrorPath),
        formatError = TaxYearFormatError.withPath(taxYearErrorPath),
        rangeError = RuleTaxYearRangeInvalidError.withPath(taxYearErrorPath)
      )(taxYearBroughtForwardFrom) andThen (_ =>
        ResolveIncompleteTaxYear(
          RuleTaxYearNotEndedError.withPath(taxYearErrorPath)
        ).resolver(taxYearBroughtForwardFrom))

    combine(
      resolvedTaxYear,
      ResolveBusinessId(businessId),
      resolveParsedNumber(lossAmount, path = "/lossAmount")
    ).map(_ => parsed)
  }

}
