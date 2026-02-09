/*
 * Copyright 2026 HM Revenue & Customs
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

package v7.lossesAndClaims.createAmend.request

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.*
import v7.lossesAndClaims.commons.PreferenceOrder

case class Claims(carryBack: Option[CarryBack],
                  carrySideways: Option[CarrySideways],
                  preferenceOrder: Option[PreferenceOrder],
                  carryForward: Option[CarryForward])

object Claims {
  implicit val reads: Reads[Claims] = Json.reads[Claims]

  implicit val write: OWrites[Claims] = (
    (JsPath \ "carryBack").writeNullable[CarryBack] and
      (JsPath \ "carrySideways").writeNullable[CarrySideways] and
      (JsPath \ "preferenceOrderSection64").writeNullable[PreferenceOrder] and
      (JsPath \ "carryForward").writeNullable[CarryForward]
  )(o => Tuple.fromProductTyped(o))

}
