/*
 * Copyright 2019 HM Revenue & Customs
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

package v1.models.des

import play.api.libs.json.{ JsPath, Json, Reads, Writes }

case class BFLossId(id: String)

object BFLossId {
  implicit val writes: Writes[BFLossId] = Json.writes[BFLossId]

  implicit val reads: Reads[BFLossId] = (JsPath \ "lossId").read[String].map(BFLossId(_))
}

case class RetrieveBFLossesResponse(losses: Seq[BFLossId])

object RetrieveBFLossesResponse {
  implicit val writes: Writes[RetrieveBFLossesResponse] =
    Json.writes[RetrieveBFLossesResponse]

  implicit val reads: Reads[RetrieveBFLossesResponse] =
    implicitly[Reads[Seq[BFLossId]]].map(RetrieveBFLossesResponse(_))
}
