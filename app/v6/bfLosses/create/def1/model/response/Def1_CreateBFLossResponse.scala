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

package v6.bfLosses.create.def1.model.response

import play.api.libs.json._
import v6.bfLosses.create.model.response.CreateBFLossResponse

case class Def1_CreateBFLossResponse(lossId: String) extends CreateBFLossResponse

object Def1_CreateBFLossResponse {

  implicit val reads: Reads[Def1_CreateBFLossResponse] =
    (__ \ "lossId").read[String].map(Def1_CreateBFLossResponse.apply)

  implicit val writes: OWrites[Def1_CreateBFLossResponse] = Json.writes[Def1_CreateBFLossResponse]

}
