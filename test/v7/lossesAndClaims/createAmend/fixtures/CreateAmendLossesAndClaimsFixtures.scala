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

package v7.lossesAndClaims.createAmend.fixtures

import play.api.libs.json.{JsValue, Json}
import v7.lossesAndClaims.createAmend.request.*

object CreateAmendLossesAndClaimsFixtures {

  val carryBack: CarryBack = CarryBack(
    previousYearGeneralIncome = Some(5000.99),
    earlyYearLosses = Some(5000.99),
    terminalLosses = Some(5000.99)
  )

  val carrySideways: CarrySideways = CarrySideways(currentYearGeneralIncome = Some(5000.99))

  val preferenceOrder: PreferenceOrder = PreferenceOrder(applyFirst = Some("carry-sideways"))

  val carryForward: CarryForward = CarryForward(currentYearLosses = Some(5000.99), previousYearsLosses = Some(5000.99))

  val claims: Claims = Claims(
    carryBack = Some(carryBack),
    carrySideways = Some(carrySideways),
    preferenceOrder = Some(preferenceOrder),
    carryForward = Some(carryForward)
  )

  val losses: Losses = Losses(broughtForwardLosses = Some(5000.99))

  val requestBodyModel: CreateAmendLossesAndClaimsRequestBody = CreateAmendLossesAndClaimsRequestBody(
    claims = Some(claims.copy(carryBack = Some(carryBack.copy(terminalLosses = None)))),
    losses = Some(losses)
  )

  val requestBodyJson: JsValue = Json.parse(
    """
      |{
      |  "claims": {
      |    "carryBack": {
      |      "previousYearGeneralIncome": 5000.99,
      |      "earlyYearLosses": 5000.99
      |    },
      |    "carrySideways": {
      |      "currentYearGeneralIncome": 5000.99
      |    },
      |    "preferenceOrder": {
      |      "applyFirst": "carry-sideways"
      |    },
      |    "carryForward": {
      |      "currentYearLosses": 5000.99,
      |      "previousYearsLosses": 5000.99
      |    }
      |  },
      |  "losses": {
      |    "broughtForwardLosses": 5000.99
      |  }
      |}
    """.stripMargin
  )

}
