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

import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers._
import api.models.domain.TodaySupplier
import shared.models.errors._
import cats.data.Validated
import cats.implicits._
import play.api.libs.json.JsValue
import v4.controllers.validators.resolvers.ResolveBFTypeOfLossFromJson
import v4.models.request.createBFLosses.{CreateBFLossRequestBody, CreateBFLossRequestData}

import javax.inject.{Inject, Singleton}

@Singleton
class CreateBFLossValidatorFactory @Inject()(implicit todaySupplier: TodaySupplier = new TodaySupplier) {

  private val resolveJson = new ResolveJsonObject[CreateBFLossRequestBody]()
  private val resolveParsedNumber = ResolveParsedNumber()
  private val resolveTaxYear = DetailedResolveTaxYear(allowIncompleteTaxYear = false, maybeMinimumTaxYear = Some(minimumTaxYearBFLoss))

  def validator(nino: String, body: JsValue): Validator[CreateBFLossRequestData] =
    new Validator[CreateBFLossRequestData] {

      def validate: Validated[Seq[MtdError], CreateBFLossRequestData] =
        ResolveBFTypeOfLossFromJson(body, None, errorPath = Some("/typeOfLoss"))
          .andThen(_ =>
            (
              ResolveNino(nino),
              resolveJson(body)
            ).mapN(CreateBFLossRequestData)
              .andThen(validateParsedData))

      private def validateParsedData(parsed: CreateBFLossRequestData): Validated[Seq[MtdError], CreateBFLossRequestData] =
        combine(
          resolveTaxYear(parsed.broughtForwardLoss.taxYearBroughtForwardFrom, None, Some("/taxYearBroughtForwardFrom")),
          ResolveBusinessId(parsed.broughtForwardLoss.businessId),
          resolveParsedNumber(parsed.broughtForwardLoss.lossAmount, path = "/lossAmount")
        ).map(_ => parsed)

    }

}
