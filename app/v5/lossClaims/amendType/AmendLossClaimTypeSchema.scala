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

package v5.lossClaims.amendType

import play.api.libs.json.Reads
import schema.DownstreamReadable
import v5.lossClaims.amendType.def1.model.response.Def1_AmendLossClaimTypeResponse
import v5.lossClaims.amendType.model.response.AmendLossClaimTypeResponse

sealed trait AmendLossClaimTypeSchema extends DownstreamReadable[AmendLossClaimTypeResponse]

object AmendLossClaimTypeSchema {

  case object Def1 extends AmendLossClaimTypeSchema {
    type DownstreamResp = Def1_AmendLossClaimTypeResponse
    val connectorReads: Reads[DownstreamResp] = Def1_AmendLossClaimTypeResponse.reads
  }

  val schema: AmendLossClaimTypeSchema = Def1

}
