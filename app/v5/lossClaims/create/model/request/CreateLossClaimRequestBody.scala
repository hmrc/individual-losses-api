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

package v5.lossClaims.create.model.request

import play.api.libs.json._
import shared.config.SharedAppConfig
import shared.utils.JsonWritesUtil
import v5.lossClaims.create.def1.model.request.Def1_CreateLossClaimRequestBody

trait CreateLossClaimRequestBody

object CreateLossClaimRequestBody extends JsonWritesUtil {

  implicit def writes(implicit appConfig: SharedAppConfig): OWrites[CreateLossClaimRequestBody] = writesFrom {
    case a: Def1_CreateLossClaimRequestBody =>
      implicitly[OWrites[Def1_CreateLossClaimRequestBody]].writes(a)
  }

}
