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

package v3.controllers.validators

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.{DetailedResolveTaxYear, ResolveBusinessId, ResolveNino}
import api.models.errors._
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import config.FixedConfig
import v3.models.domain.bfLoss.TypeOfLoss._
import v3.models.domain.bfLoss.{IncomeSourceType, TypeOfLoss}
import v3.models.request.listBFLosses.ListBFLossesRequestData

import javax.inject.Singleton

@Singleton
class ListBFLossesValidatorFactory extends FixedConfig {

  private val resolveTaxYear = DetailedResolveTaxYear(maybeMinimumTaxYear = Some(minimumTaxYearBFLoss))

  // only allow single self employment loss type - so main loss type validator does not quite do it for us
  private val availableLossTypeNames =
    Seq(`uk-property-fhl`, `uk-property-non-fhl`, `self-employment`, `foreign-property-fhl-eea`, `foreign-property`).map(_.toString)

  def validator(nino: String,
                taxYearBroughtForwardFrom: Option[String],
                typeOfLoss: Option[String],
                businessId: Option[String]): Validator[ListBFLossesRequestData] =
    new Validator[ListBFLossesRequestData] {

      def validate: Validated[Seq[MtdError], ListBFLossesRequestData] =
        (
          ResolveNino(nino),
          resolveTaxYear(taxYearBroughtForwardFrom),
          resolveIncomeSourceType,
          ResolveBusinessId(businessId)
        ).mapN(ListBFLossesRequestData)

      private def resolveIncomeSourceType: Validated[Seq[MtdError], Option[IncomeSourceType]] =
        typeOfLoss
          .map {
            case lossType if availableLossTypeNames.contains(lossType) =>
              Valid(TypeOfLoss.parser.lift(lossType).flatMap(_.toIncomeSourceType))
            case _ =>
              Invalid(List(TypeOfLossFormatError))
          }
          .getOrElse(Valid(None))

    }

}
