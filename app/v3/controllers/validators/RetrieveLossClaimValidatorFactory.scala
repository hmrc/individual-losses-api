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
import api.controllers.validators.resolvers.ResolveNino
import api.models.errors.MtdError
import cats.data.Validated
import cats.implicits._
import v3.controllers.validators.resolvers.ResolveLossClaimId
import v3.models.request.retrieveLossClaim.RetrieveLossClaimRequestData

import javax.inject.Singleton

@Singleton
class RetrieveLossClaimValidatorFactory {

  def validator(nino: String, claimId: String): Validator[RetrieveLossClaimRequestData] =
    new Validator[RetrieveLossClaimRequestData] {

      def validate: Validated[Seq[MtdError], RetrieveLossClaimRequestData] = {
        (
          ResolveNino(nino),
          ResolveLossClaimId(claimId)
        ).mapN(RetrieveLossClaimRequestData)

      }

    }

}
