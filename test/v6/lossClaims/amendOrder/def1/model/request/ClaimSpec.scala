/*
 * Copyright 2025 HM Revenue & Customs
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

package v6.lossClaims.amendOrder.def1.model.request

import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec
import v6.lossClaims.amendOrder.def1.model.request.Claim

class ClaimSpec extends UnitSpec {

  val claim: Claim = Claim(
    claimId = "234568790ABCDE",
    sequence = 1
  )

  val claimJson: JsValue = Json.parse("""
    |{
    | "claimId": "234568790ABCDE",
    | "sequence": 1
    |}
    |""".stripMargin)

  val downstreamJson: JsValue = Json.parse("""
    |{
    | "claimId": "234568790ABCDE",
    | "sequence": 1
    |}
    |""".stripMargin)

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        claim shouldBe claimJson.as[Claim]
      }
    }
  }

  "writes" when {
    "passed valid model" should {
      "return valid json" in {
        Json.toJson(claim) shouldBe downstreamJson
      }
    }
  }

}
