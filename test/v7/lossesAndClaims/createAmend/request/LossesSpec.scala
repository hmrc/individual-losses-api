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

class LossesSpec extends UnitSpec {

  val requestBody: Losses = Losses(
    Option(5000.99)
  )

  val defaultMtdRequestJson: JsValue = Json.parse(s"""
       |{
       |      "broughtForwardLosses": 5000.99
       |}
       |""".stripMargin)

  "Json Validate" should {
    "read a default model from JSON" in {
      val result = defaultMtdRequestJson.validate[Losses]
      result.isSuccess shouldBe true
    }
  }

}
