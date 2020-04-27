/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.models.domain

import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v1.models.des.ReliefClaimed

class LossClaimsListSpec extends UnitSpec {

  val lossClaimsList = LossClaimsList(
    claimType = ReliefClaimed.`CSGI`,
    listOfLossClaims = Seq(Claim(
      id = "234568790ABCDE",
      sequence = 1
    ),
      Claim(
        id = "234568790ABCFE",
        sequence = 2
      )
    )
  )

  val mtdLossClaimsListJson : JsValue = Json.parse("""
    |{
    |   "claimType" : "carry-sideways",
    |   "listOfLossClaims": [
    |      {
    |      "id": "234568790ABCDE",
    |      "sequence":1
    |      },
    |      {
    |      "id": "234568790ABCFE",
    |      "sequence":2
    |      }
    |   ]
    |}
    |""".stripMargin
  )


  val desLossClaimsListJson : JsValue = Json.parse("""
    |{
    |   "claimType" : "CSGI",
    |   "listOfLossClaims": [
    |      {
    |      "id": "234568790ABCDE",
    |      "sequence":1
    |      },
    |      {
    |      "id": "234568790ABCFE",
    |      "sequence":2
    |      }
    |   ]
    |}
    |""".stripMargin
  )

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        lossClaimsList shouldBe mtdLossClaimsListJson.as[LossClaimsList]
      }
    }
  }
  "writes" when {
    "passed valid model" should {
      "return valid json" in {
        Json.toJson(lossClaimsList) shouldBe desLossClaimsListJson
      }
    }
  }
}
