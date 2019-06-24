/*
 * Copyright 2019 HM Revenue & Customs
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

package v1.models.des

import play.api.libs.json.Json
import support.UnitSpec

class AmendBroughtForwardLossResponseSpec extends UnitSpec {
  val desJson = Json.parse(
    """
       |{
       |  "incomeSourceId": "000000000000001",
       |  "lossType": "INCOME",
       |  "broughtForwardLossAmount": 99999999999.99,
       |  "taxYear": "2020"
       |}
     """.stripMargin)
  val desToModel = AmendBroughtForwardLossResponse(selfEmploymentId = "000000000000001", typeOfLoss = "INCOME", lossAmount = 99999999999.99, taxYear = "2020")
  val modelToMtd = AmendBroughtForwardLossResponse(selfEmploymentId = "000000000000001", typeOfLoss = "INCOME", lossAmount = 99999999999.99, taxYear = "2019-20")
  val mtdJson = Json.parse(
    """{
       |  "selfEmploymentId": "000000000000001",
       |  "typeOfLoss": "INCOME",
       |  "lossAmount": 99999999999.99,
       |  "taxYear": "2019-20"
       |}
     """.stripMargin)

  "Json Reads" should {
    "convert JSON from DES into a valid model" in {
      desJson.as[AmendBroughtForwardLossResponse] shouldBe desToModel
    }
  }
  "Json Writes" should {
    "convert a valid model into MTD JSON" in {
      Json.toJson(modelToMtd) shouldBe mtdJson
    }
  }
}
