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

class ClaimsSpec extends UnitSpec {

  val requestBody: Claims = Claims(
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
  )

  val defaultMtdRequestJson: JsValue = Json.parse(s"""
       |{
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
       |  }
       |""".stripMargin)

  val defaultDownstreamRequestJson: JsValue = Json.parse(s"""
       |{
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
       |  }
       |""".stripMargin)

  "Json Reads" should {
    "read a default model from JSON" in {
      defaultMtdRequestJson.as[Claims] shouldBe requestBody
    }
  }

  "Json Writes" should {
    "write a default model to JSON" in {
      Json.toJson(requestBody) shouldBe defaultDownstreamRequestJson
    }
  }

}
