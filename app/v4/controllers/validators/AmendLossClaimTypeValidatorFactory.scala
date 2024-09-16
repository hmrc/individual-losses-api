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
import cats.implicits._
import common.errors.{ClaimIdFormatError, TypeOfClaimFormatError}
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveJsonObject, ResolveNino, ResolveStringPattern}
import shared.models.errors.MtdError
import v4.controllers.validators.resolvers.ResolveLossTypeOfClaimFromJson
import v4.models.domain.lossClaim.ClaimId
import v4.models.request.amendLossClaimType.{AmendLossClaimTypeRequestBody, AmendLossClaimTypeRequestData}

import javax.inject.Singleton

@Singleton
class AmendLossClaimTypeValidatorFactory {

  private val resolveClaimId = new ResolveStringPattern("^[A-Za-z0-9]{15}$".r, ClaimIdFormatError)
  private val resolveJson    = new ResolveJsonObject[AmendLossClaimTypeRequestBody]()

  def validator(nino: String, claimId: String, body: JsValue): Validator[AmendLossClaimTypeRequestData] =
    new Validator[AmendLossClaimTypeRequestData] {

      def validate: Validated[Seq[MtdError], AmendLossClaimTypeRequestData] =
        ResolveLossTypeOfClaimFromJson(body, Some(TypeOfClaimFormatError.withPath("/typeOfClaim")))
          .andThen(_ =>
            (
              ResolveNino(nino),
              resolveClaimId(claimId).map(ClaimId),
              resolveJson(body)
            ).mapN(AmendLossClaimTypeRequestData))

    }

}
