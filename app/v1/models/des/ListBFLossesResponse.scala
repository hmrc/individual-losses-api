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

import play.api.libs.json.{JsPath, Reads}
import v1.models.domain.{BFLossId, BFLosses}

case class ListBFLossesResponse(ids: Seq[String]) {
  def toMtd: BFLosses = BFLosses(ids.map(BFLossId(_)))
}

object ListBFLossesResponse {
  private case class ListBFLossesResponseItem(lossId: String) extends AnyVal

  // We ignore all fields from DES but the lossId
  private implicit val idRead: Reads[ListBFLossesResponseItem] =
    (JsPath \ "lossId").read[String].map(ListBFLossesResponseItem)

  implicit val reads: Reads[ListBFLossesResponse] =
    implicitly[Reads[Seq[ListBFLossesResponseItem]]]
      .map(items => ListBFLossesResponse(items.map(_.lossId)))
}
