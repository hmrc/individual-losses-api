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

package v4.models.request.amendLossClaimsOrder

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class Claim(claimId: String, sequence: Int)

object Claim {
  implicit val reads: Reads[Claim] = Json.reads[Claim]

  implicit val writes: Writes[Claim] = (
    (JsPath \ "claimId").write[String] and
      (JsPath \ "sequence").write[Int]
  )(w => Tuple.fromProductTyped(w))

}
