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

package v5.lossClaims.amendType.def1

import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveJsonObject, ResolveNino, ResolveStringPattern}
import api.models.errors.ClaimIdFormatError
import shared.models.errors.MtdError
import cats.data.Validated
import cats.implicits._
import play.api.libs.json.JsValue
import v4.controllers.validators.resolvers.ResolveLossTypeOfClaimFromJson
import v4.models.domain.lossClaim.ClaimId
import v5.lossClaims.amendType.def1.model.request.{Def1_AmendLossClaimTypeRequestBody, Def1_AmendLossClaimTypeRequestData}
import v5.lossClaims.amendType.model.request.AmendLossClaimTypeRequestData

class Def1_AmendLossClaimTypeValidator(nino: String, claimId: String, body: JsValue) extends Validator[AmendLossClaimTypeRequestData] {

  private val resolveClaimId = new ResolveStringPattern("^[A-Za-z0-9]{15}$".r, ClaimIdFormatError)
  private val resolveJson    = new ResolveJsonObject[Def1_AmendLossClaimTypeRequestBody]()

  def validate: Validated[Seq[MtdError], AmendLossClaimTypeRequestData] =
    ResolveLossTypeOfClaimFromJson(body, path = Some("/typeOfClaim"))
      .andThen(_ =>
        (
          ResolveNino(nino),
          resolveClaimId(claimId).map(ClaimId),
          resolveJson(body)
        ).mapN(Def1_AmendLossClaimTypeRequestData))

}
