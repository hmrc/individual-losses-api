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

package v5.bfLossClaims.list

import play.api.libs.json.Reads
import schema.DownstreamReadable
import v5.bfLossClaims.list.def1.model.response.{Def1_ListBFLossesItem, Def1_ListBFLossesResponse}
import v5.bfLossClaims.list.model.response.{ListBFLossesItem, ListBFLossesResponse}

sealed trait ListBFLossesSchema extends DownstreamReadable[ListBFLossesResponse[ListBFLossesItem]]

object ListBFLossesSchema {

  case object Def1 extends ListBFLossesSchema {
    type DownstreamResp = Def1_ListBFLossesResponse[Def1_ListBFLossesItem]
    val connectorReads: Reads[DownstreamResp] = Def1_ListBFLossesResponse.reads
  }

  val schema: ListBFLossesSchema = Def1

}
