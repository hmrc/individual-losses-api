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

package v5.lossClaims.list

import play.api.libs.json.Reads
import schema.DownstreamReadable
import v5.lossClaims.list.def1.response.Def1_ListLossClaimsResponse
import v5.lossClaims.list.model.response.ListLossClaimsResponse

sealed trait ListLossClaimsSchema extends DownstreamReadable[ListLossClaimsResponse]

object ListLossClaimsSchema {

  case object Def1 extends ListLossClaimsSchema {
    type DownstreamResp = Def1_ListLossClaimsResponse
    val connectorReads: Reads[DownstreamResp] = Def1_ListLossClaimsResponse.reads
  }

  val schema: ListLossClaimsSchema = Def1

}
