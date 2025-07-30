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

package v5.lossClaims.amendOrder.def1

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits.*
import common.errors.{TaxYearClaimedForFormatError, TypeOfClaimFormatError}
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.ResolveTaxYear.resolverWithCustomErrors
import shared.controllers.validators.resolvers.{ResolveJsonObject, ResolveNino}
import shared.models.errors.*
import v5.lossClaims.amendOrder.def1.model.request.{Def1_AmendLossClaimsOrderRequestBody, Def1_AmendLossClaimsOrderRequestData}
import v5.lossClaims.amendOrder.model.request.AmendLossClaimsOrderRequestData
import v5.lossClaims.common.models.TypeOfClaim
import v5.lossClaims.common.resolvers.ResolveLossTypeOfClaimFromJson

class Def1_AmendLossClaimsOrderValidator(nino: String, taxYearClaimedFor: String, body: JsValue) extends Validator[AmendLossClaimsOrderRequestData] {

  private val resolveJson    = new ResolveJsonObject[Def1_AmendLossClaimsOrderRequestBody]
  private val resolveTaxYear = resolverWithCustomErrors(TaxYearClaimedForFormatError, RuleTaxYearRangeInvalidError)

  def validate: Validated[Seq[MtdError], AmendLossClaimsOrderRequestData] =
    ResolveLossTypeOfClaimFromJson(body)
      .andThen(validatePermittedTypeOfClaim)
      .andThen(_ =>
        (
          ResolveNino(nino),
          resolveTaxYear(taxYearClaimedFor),
          resolveJson(body)
        ).mapN(Def1_AmendLossClaimsOrderRequestData) andThen Def1_AmendLossClaimsOrderRulesValidator.validateBusinessRules)

  private def validatePermittedTypeOfClaim(maybeTypeOfClaim: Option[TypeOfClaim]): Validated[Seq[MtdError], Unit] = {
    maybeTypeOfClaim match {
      case Some(typeOfClaim) if typeOfClaim == TypeOfClaim.`carry-sideways` =>
        Valid(())
      case Some(_) => Invalid(List(TypeOfClaimFormatError))
      case None    => Valid(())
    }
  }

}
