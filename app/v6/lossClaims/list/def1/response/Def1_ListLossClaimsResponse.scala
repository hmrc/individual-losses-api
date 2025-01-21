/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.list.def1.response

import play.api.libs.json._
import v6.lossClaims.list.model.response.ListLossClaimsResponse

case class Def1_ListLossClaimsResponse(claims: Seq[ListLossClaimsItem]) extends ListLossClaimsResponse

object Def1_ListLossClaimsResponse {

  implicit val writes: OWrites[Def1_ListLossClaimsResponse] =
    Json.writes[Def1_ListLossClaimsResponse]

  implicit val reads: Reads[Def1_ListLossClaimsResponse] =
    implicitly[Reads[Seq[ListLossClaimsItem]]].map(Def1_ListLossClaimsResponse(_))

}
