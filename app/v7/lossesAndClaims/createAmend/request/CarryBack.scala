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
import play.api.libs.json.{JsPath, Json, OWrites, Reads}

case class CarryBack(previousYearGeneralIncome: Option[BigDecimal], earlyYearLosses: Option[BigDecimal], terminalLosses: Option[BigDecimal])

object CarryBack {
  implicit val reads: Reads[CarryBack] = Json.reads[CarryBack]

  implicit val write: OWrites[CarryBack] = (
    (JsPath \ "previousYearGeneralIncomeSection64").writeNullable[BigDecimal] and
      (JsPath \ "earlyYearLossesSection72").writeNullable[BigDecimal] and
      (JsPath \ "terminalLossesSection89").writeNullable[BigDecimal]
  )(o => Tuple.fromProductTyped(o))

}
