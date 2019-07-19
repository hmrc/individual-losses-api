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

package v1.models.domain

import play.api.libs.json.{JsObject, Json, OWrites, Reads}

case class AmendLossClaim(typeOfClaim: TypeOfClaim)

object AmendLossClaim {
  implicit val reads: Reads[AmendLossClaim] = Json.reads[AmendLossClaim]
  implicit val writes: OWrites[AmendLossClaim] = new OWrites[AmendLossClaim] {
    override def writes(o: AmendLossClaim): JsObject = Json.obj(
      "reliefClaimed" -> o.typeOfClaim.toReliefClaimed
    )
  }

}