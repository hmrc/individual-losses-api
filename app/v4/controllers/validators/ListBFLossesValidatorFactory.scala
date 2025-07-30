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
import shared.controllers.validators.resolvers.{ResolveBusinessId, ResolveNino, ResolveTaxYearMinimum, ResolverSupport}
import shared.models.errors.*
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.*
import common.errors.TypeOfLossFormatError
import v4.models.domain.bfLoss.TypeOfLoss.*
import v4.models.domain.bfLoss.{IncomeSourceType, TypeOfLoss}
import v4.models.request.listLossClaims.ListBFLossesRequestData

import javax.inject.Singleton

@Singleton
class ListBFLossesValidatorFactory {

  private val resolveTaxYear = ResolveTaxYearMinimum(minimumTaxYearBFLoss)

  // only allow single self employment loss type - so main loss type validator does not quite do it for us
  private val availableLossTypeNames =
    Seq(`uk-property-fhl`, `uk-property-non-fhl`, `self-employment`, `foreign-property-fhl-eea`, `foreign-property`).map(_.toString)

  def validator(nino: String,
                taxYearBroughtForwardFrom: String,
                typeOfLoss: Option[String],
                businessId: Option[String]): Validator[ListBFLossesRequestData] =
    new Validator[ListBFLossesRequestData] with ResolverSupport {

      def validate: Validated[Seq[MtdError], ListBFLossesRequestData] =
        (
          ResolveNino(nino),
          resolveTaxYear(taxYearBroughtForwardFrom),
          resolveIncomeSourceType,
          ResolveBusinessId.resolver.resolveOptionally(businessId)
        ).mapN(ListBFLossesRequestData)

      private def resolveIncomeSourceType: Validated[Seq[MtdError], Option[IncomeSourceType]] =
        typeOfLoss
          .map(lossType =>
            if (availableLossTypeNames.contains(lossType)) {
              Valid(TypeOfLoss.parser.lift(lossType).flatMap(_.toIncomeSourceType))
            } else {
              Invalid(List(TypeOfLossFormatError))
            })
          .getOrElse(Valid(None))

    }

}
