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

import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec
import v7.lossesAndClaims.createAmend.fixtures.CreateAmendLossesAndClaimsFixtures.claims

class ClaimsSpec extends UnitSpec {

  private val mtdJson: JsValue = Json.parse(
    """
      |{
      |  "carryBack": {
      |    "previousYearGeneralIncome": 5000.99,
      |    "earlyYearLosses": 5000.99,
      |    "terminalLosses": 5000.99
      |  },
      |  "carrySideways": {
      |    "currentYearGeneralIncome": 5000.99
      |  },
      |  "preferenceOrder": {
      |    "applyFirst": "carry-sideways"
      |  },
      |  "carryForward": {
      |    "currentYearLosses": 5000.99,
      |    "previousYearsLosses": 5000.99
      |  }
      |}
    """.stripMargin
  )

  private val downstreamJson: JsValue = Json.parse(
    """
      |{
      |  "carryBack": {
      |    "previousYearGeneralIncomeSection64": 5000.99,
      |    "earlyYearLossesSection72": 5000.99,
      |    "terminalLossesSection89": 5000.99
      |  },
      |  "carrySideways": {
      |    "currentYearGeneralIncomeSection64": 5000.99
      |  },
      |  "preferenceOrderSection64": {
      |    "applyFirst": "carry-sideways"
      |  },
      |  "carryForward": {
      |    "currentYearLossesSection83": 5000.99,
      |    "previousYearsLossesSection83": 5000.99
      |  }
      |}
    """.stripMargin
  )

  "Json Reads" should {
    "read model from JSON" in {
      mtdJson.as[Claims] shouldBe claims
    }
  }

  "Json Writes" should {
    "write model to JSON" in {
      Json.toJson(claims) shouldBe downstreamJson
    }
  }

}
