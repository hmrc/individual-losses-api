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

package v7.lossesAndClaims.retrieve.model.response

import play.api.libs.json.{JsValue, Json}
import shared.config.MockSharedAppConfig
import shared.utils.UnitSpec
import v7.lossesAndClaims.retrieve.model.response.PreferenceOrderEnum.{`carry-back`, `carry-sideways`}

class RetrieveLossesAndClaimsResponseSpec extends UnitSpec with MockSharedAppConfig {

  val nino: String    = "AA123456A"
  val taxYear: String = "2026-27"
  val claimId: String = "claimId"

  private val lastModified = "2026-08-24T14:15:22.544Z"

  val defaultDownstreamResponseJson: JsValue = Json.parse(s"""
       |{
       |  "submittedOn": "$lastModified",
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

  val defaultMtdResponseJson: JsValue = Json.parse(s"""
       |{
       |  "submittedOn": "$lastModified",
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

  private val defaultRetrieveResponse: RetrieveLossesAndClaimsResponse = RetrieveLossesAndClaimsResponse(
    "2026-08-24T14:15:22.544Z",
    Some(
      Claims(
        Some(
          CarryBack(
            Some(5000.99),
            Some(5000.99),
            None
          )),
        Some(
          CarrySideways(
            Some(5000.99)
          )),
        Some(
          PreferenceOrder(
            Some(`carry-back`)
          )),
        Some(
          CarryForward(
            Some(5000.99),
            Some(5000.99)
          ))
      )),
    Some(
      Losses(
        Some(5000.99)
      ))
  )

  "Json Reads" should {
    "read a default model from JSON" in {
      defaultDownstreamResponseJson.as[RetrieveLossesAndClaimsResponse] shouldBe defaultRetrieveResponse
    }
  }

  "Json Writes" should {
    "write a default model to JSON" in {
      Json.toJson(defaultRetrieveResponse) shouldBe defaultMtdResponseJson
    }
  }

  val terminalLossesDownstreamResponseJson: JsValue = Json.parse(s"""
       |{
       |  "submittedOn": "$lastModified",
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

  val terminalLossesMtdResponseJson: JsValue = Json.parse(s"""
       |{
       |  "submittedOn": "$lastModified",
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

  private val terminalLossesRetrieveResponse: RetrieveLossesAndClaimsResponse = RetrieveLossesAndClaimsResponse(
    "2026-08-24T14:15:22.544Z",
    Some(
      Claims(
        Some(
          CarryBack(
            Some(5000.99),
            Some(5000.99),
            Some(5000.99)
          )),
        Some(
          CarrySideways(
            Some(5000.99)
          )),
        Some(
          PreferenceOrder(
            Some(`carry-sideways`)
          )),
        Some(
          CarryForward(
            Some(5000.99),
            Some(5000.99)
          ))
      )),
    Some(
      Losses(
        Some(5000.99)
      ))
  )

  "Json Reads" should {
    "read a terminal losses model from JSON" in {
      terminalLossesDownstreamResponseJson.as[RetrieveLossesAndClaimsResponse] shouldBe terminalLossesRetrieveResponse
    }
  }

  "Json Writes" should {
    "write a terminal losses model to JSON" in {
      Json.toJson(terminalLossesRetrieveResponse) shouldBe terminalLossesMtdResponseJson
    }
  }

}
