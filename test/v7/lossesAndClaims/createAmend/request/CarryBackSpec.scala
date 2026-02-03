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

class CarryBackSpec extends UnitSpec {

  val carryBackRequestBody: CarryBack = CarryBack(
    Option(5000.99),
    Option(5000.99),
    Option(5000.99)
  )

  val defaultMtdRequestJson: JsValue = Json.parse(s"""
       |{
       |     "previousYearGeneralIncome": 5000.99,
       |     "earlyYearLosses": 5000.99,
       |     "terminalLosses": 5000.99
       |}
       |""".stripMargin)

  val defaultDownstreamRequestJson: JsValue = Json.parse(s"""
       |{
       |     "previousYearGeneralIncomeSection64": 5000.99,
       |     "earlyYearLossesSection72": 5000.99,
       |     "terminalLossesSection89": 5000.99
       |}
       |""".stripMargin)

  "Json Reads" should {
    "read a default model from JSON" in {
      defaultMtdRequestJson.as[CarryBack] shouldBe carryBackRequestBody
    }
  }

  "Json Writes" should {
    "write a default model to JSON" in {
      Json.toJson(carryBackRequestBody) shouldBe defaultDownstreamRequestJson
    }
  }

}
