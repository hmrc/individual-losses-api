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

package v6.bfLosses.create.def1

import cats.data.Validated
import cats.implicits.catsSyntaxTuple3Semigroupal
import common.errors.TypeOfLossFormatError
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers._
import shared.models.domain.TaxYear
import shared.models.errors._
import v6.bfLosses.common.minimumTaxYear
import v6.bfLosses.common.resolvers.ResolveBFTypeOfLossFromJson
import v6.bfLosses.create.def1.model.request.{Def1_CreateBFLossRequestBody, Def1_CreateBFLossRequestData}
import v6.bfLosses.create.model.request.CreateBFLossRequestData

import java.time.Clock
import javax.inject.Inject

class Def1_CreateBFLossValidator @Inject() (nino: String, taxYear: String, body: JsValue)(implicit val clock: Clock = Clock.systemUTC())
    extends Validator[CreateBFLossRequestData] {

  private val resolveJson         = new ResolveJsonObject[Def1_CreateBFLossRequestBody]()
  private val resolveParsedNumber = ResolveParsedNumber()

  def resolvedTaxYear(taxYear: String, taxYearErrorPath: Option[String] = None): Validated[Seq[MtdError], TaxYear] = {
    def withPath(error: MtdError): MtdError = taxYearErrorPath.fold(error)(error.withPath)

    ResolveTaxYearMinimum(
      minimumTaxYear,
      notSupportedError = withPath(RuleTaxYearNotSupportedError),
      formatError = withPath(TaxYearFormatError),
      rangeError = withPath(RuleTaxYearRangeInvalidError)
    )(taxYear)
  }

  def validate: Validated[Seq[MtdError], CreateBFLossRequestData] =
    ResolveBFTypeOfLossFromJson(body, Some(TypeOfLossFormatError.withPath("/typeOfLoss")))
      .andThen(_ =>
        (
          ResolveNino(nino),
          resolvedTaxYear(taxYear),
          resolveJson(body)
        ).mapN(Def1_CreateBFLossRequestData)
          .andThen(validateParsedData))

  private def validateParsedData(parsed: Def1_CreateBFLossRequestData): Validated[Seq[MtdError], CreateBFLossRequestData] = {
    import parsed.broughtForwardLoss._
    val taxYearErrorPath = "/taxYearBroughtForwardFrom"

    combine(
      resolvedTaxYear(taxYearBroughtForwardFrom, Some(taxYearErrorPath)).andThen(_ =>
        ResolveIncompleteTaxYear(
          RuleTaxYearNotEndedError.withPath(taxYearErrorPath)
        ).apply(taxYearBroughtForwardFrom)),
      ResolveBusinessId(businessId),
      resolveParsedNumber(lossAmount, path = "/lossAmount")
    ).map(_ => parsed)
  }

}
