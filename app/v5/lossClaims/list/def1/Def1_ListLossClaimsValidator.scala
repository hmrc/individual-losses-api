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

package v5.lossClaims.list.def1

import api.controllers.validators.Validator
import api.controllers.validators.resolvers.{DetailedResolveTaxYear, ResolveBusinessId, ResolveNino}
import api.models.errors._
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import v4.controllers.validators.minimumTaxYearLossClaim
import v4.controllers.validators.resolvers.{ResolveLossClaimTypeOfLoss, ResolveLossTypeOfClaim}
import v4.models.domain.lossClaim.TypeOfClaim
import v5.lossClaims.list.def1.request.Def1_ListLossClaimsRequestData
import v5.lossClaims.list.model.request.ListLossClaimsRequestData

class Def1_ListLossClaimsValidator(nino: String,
                                   taxYearClaimedFor: String,
                                   typeOfLoss: Option[String],
                                   businessId: Option[String],
                                   typeOfClaim: Option[String])
    extends Validator[ListLossClaimsRequestData] {

  private val resolveTaxYear = DetailedResolveTaxYear(maybeMinimumTaxYear = Some(minimumTaxYearLossClaim))

  def validate: Validated[Seq[MtdError], ListLossClaimsRequestData] = {
    (
      ResolveNino(nino),
      resolveTaxYear(taxYearClaimedFor),
      ResolveLossClaimTypeOfLoss(typeOfLoss),
      ResolveBusinessId(businessId),
      resolveTypeOfClaim
    ).mapN(Def1_ListLossClaimsRequestData)

  }

  private def resolveTypeOfClaim: Validated[Seq[MtdError], Option[TypeOfClaim]] =
    ResolveLossTypeOfClaim(typeOfClaim) andThen {
      case Some(parsedTypeOfClaim) =>
        if (parsedTypeOfClaim == TypeOfClaim.`carry-sideways`)
          Valid(Some(parsedTypeOfClaim))
        else
          Invalid(List(TypeOfClaimFormatError))

      case None =>
        Valid(None)
    }

}
