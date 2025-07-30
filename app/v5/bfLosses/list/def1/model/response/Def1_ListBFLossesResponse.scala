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

package v5.bfLosses.list.def1.model.response

import play.api.libs.json.*
import v5.bfLosses.list.model.response.ListBFLossesResponse

case class Def1_ListBFLossesResponse(losses: Seq[ListBFLossesItem]) extends ListBFLossesResponse {
  override def isEmpty: Boolean = losses.isEmpty
}

object Def1_ListBFLossesResponse {

  implicit val writes: OWrites[Def1_ListBFLossesResponse] =
    Json.writes[Def1_ListBFLossesResponse]

  implicit val reads: Reads[Def1_ListBFLossesResponse] =
    implicitly[Reads[Seq[ListBFLossesItem]]].map(Def1_ListBFLossesResponse(_))

}
