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

class CreateAmendLossesAndClaimsRequestBodySpec extends UnitSpec {

  val nino: String       = "AA123456A"
  val taxYear: String    = "2026-27"
  val businessId: String = "X0IS12345678901"

  val defaultDownstreamRequestJson: JsValue = Json.parse(s"""
       |{
       |  "claims": {
       |    "carryBack": {
       |      "previousYearGeneralIncomeSection64": 5000.99,
       |      "earlyYearLossesSection72": 5000.99
       |    },
       |    "carrySideways": {
       |      "currentYearGeneralIncomeSection64": 5000.99
       |    },
       |    "preferenceOrderSection64": {
       |      "applyFirst": "carry-back"
       |    },
       |    "carryForward": {
       |      "currentYearLossesSection83": 5000.99,
       |      "previousYearsLossesSection83": 5000.99
       |    }
       |  },
       |  "losses": {
       |    "broughtForwardLosses": 5000.99
       |  }
       |}
            """.stripMargin)

  val defaultMtdRequestJson: JsValue = Json.parse(s"""
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
       |      "applyFirst": "carry-back"
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
            """.stripMargin)

  private val defaultCreateAmendRequestBody: CreateAmendLossesAndClaimsRequestBody = CreateAmendLossesAndClaimsRequestBody(
    Option(
      Claims(
        Option(
          CarryBack(
            Option(5000.99),
            Option(5000.99),
            None
          )),
        Option(
          CarrySideways(
            Option(5000.99)
          )),
        Option(
          PreferenceOrder(
            Option("carry-back")
          )),
        Option(
          CarryForward(
            Option(5000.99),
            Option(5000.99)
          ))
      )),
    Option(
      Losses(
        Option(5000.99)
      ))
  )

  "Json Reads" should {
    "read a default model from JSON" in {
      defaultMtdRequestJson.as[CreateAmendLossesAndClaimsRequestBody] shouldBe defaultCreateAmendRequestBody
    }
  }

  "Json Writes" should {
    "write a default model to JSON" in {
      Json.toJson(defaultCreateAmendRequestBody) shouldBe defaultDownstreamRequestJson
    }
  }

  val terminalLossesDownstreamRequestJson: JsValue = Json.parse(s"""
       |{
       |  "claims": {
       |    "carryBack": {
       |      "previousYearGeneralIncomeSection64": 5000.99,
       |      "earlyYearLossesSection72": 5000.99,
       |      "terminalLossesSection89": 5000.99
       |    },
       |    "carrySideways": {
       |      "currentYearGeneralIncomeSection64": 5000.99
       |    },
       |    "preferenceOrderSection64": {
       |      "applyFirst": "carry-sideways"
       |    },
       |    "carryForward": {
       |      "currentYearLossesSection83": 5000.99,
       |      "previousYearsLossesSection83": 5000.99
       |    }
       |  },
       |  "losses": {
       |    "broughtForwardLosses": 5000.99
       |  }
       |}
              """.stripMargin)

  val terminalLossesMtdRequestJson: JsValue = Json.parse(s"""
       |{
       |  "claims": {
       |    "carryBack": {
       |      "previousYearGeneralIncome": 5000.99,
       |      "earlyYearLosses": 5000.99,
       |      "terminalLosses": 5000.99
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
              """.stripMargin)

  private val terminalLossesCreateAmendRequest: CreateAmendLossesAndClaimsRequestBody = CreateAmendLossesAndClaimsRequestBody(
    Option(
      Claims(
        Option(
          CarryBack(
            Option(5000.99),
            Option(5000.99),
            Option(5000.99)
          )),
        Option(
          CarrySideways(
            Option(5000.99)
          )),
        Option(
          PreferenceOrder(
            Option("carry-sideways")
          )),
        Option(
          CarryForward(
            Option(5000.99),
            Option(5000.99)
          ))
      )),
    Option(
      Losses(
        Option(5000.99)
      ))
  )

  "Json Reads" should {
    "read a terminal losses model from JSON" in {
      terminalLossesMtdRequestJson.as[CreateAmendLossesAndClaimsRequestBody] shouldBe terminalLossesCreateAmendRequest
    }
  }

  "Json Writes" should {
    "write a terminal losses model to JSON" in {
      Json.toJson(terminalLossesCreateAmendRequest) shouldBe terminalLossesDownstreamRequestJson
    }
  }

}
