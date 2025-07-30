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

package v6.bfLosses.list.def1

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.*
import common.errors.TypeOfLossFormatError
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveBusinessId, ResolveNino, ResolveTaxYearMinimum, ResolverSupport}
import shared.models.errors.*
import v6.bfLosses.common.domain.TypeOfLoss.*
import v6.bfLosses.common.domain.{IncomeSourceType, TypeOfLoss}
import v6.bfLosses.common.minimumTaxYear
import v6.bfLosses.list.def1.model.request.Def1_ListBFLossesRequestData
import v6.bfLosses.list.model.request.ListBFLossesRequestData

import javax.inject.Singleton

@Singleton
class Def1_ListBFLossesValidator(nino: String, taxYearBroughtForwardFrom: String, typeOfLoss: Option[String], businessId: Option[String])
    extends Validator[ListBFLossesRequestData]
    with ResolverSupport {

  private val resolveTaxYear = ResolveTaxYearMinimum(minimumTaxYear)

  // only allow single self employment loss type - so main loss type validator does not quite do it for us
  private val availableLossTypeNames =
    Seq(`uk-property-fhl`, `uk-property`, `self-employment`, `foreign-property-fhl-eea`, `foreign-property`).map(_.toString)

  def validate: Validated[Seq[MtdError], ListBFLossesRequestData] = {
    (
      ResolveNino(nino),
      resolveTaxYear(taxYearBroughtForwardFrom),
      resolveIncomeSourceType,
      ResolveBusinessId.resolver.resolveOptionally(businessId)
    ).mapN(Def1_ListBFLossesRequestData)
  }

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
