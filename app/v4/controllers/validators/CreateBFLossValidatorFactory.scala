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

package v4.controllers.validators

import cats.data.Validated
import cats.implicits.*
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.*
import shared.models.errors.*
import v4.controllers.validators.resolvers.ResolveBFTypeOfLossFromJson
import v4.models.request.createBFLosses.{CreateBFLossRequestBody, CreateBFLossRequestData}

import java.time.Clock
import javax.inject.Singleton

@Singleton
class CreateBFLossValidatorFactory {

  private val resolveJson = new ResolveJsonObject[CreateBFLossRequestBody]()

  def validator(nino: String, body: JsValue)(implicit clock: Clock = Clock.systemUTC()): Validator[CreateBFLossRequestData] =
    new Validator[CreateBFLossRequestData] {

      def validate: Validated[Seq[MtdError], CreateBFLossRequestData] =
        ResolveBFTypeOfLossFromJson(body, None, errorPath = Some("/typeOfLoss"))
          .andThen(_ =>
            (
              ResolveNino(nino),
              resolveJson(body)
            ).mapN(CreateBFLossRequestData)
              .andThen(validateParsedData))

      private def validateParsedData(parsed: CreateBFLossRequestData): Validated[Seq[MtdError], CreateBFLossRequestData] = {
        import parsed.broughtForwardLoss.*
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
          ResolveBusinessId(parsed.broughtForwardLoss.businessId),
          ResolveParsedNumber()(parsed.broughtForwardLoss.lossAmount, path = "/lossAmount")
        ).map(_ => parsed)
      }

    }

}
