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

import play.api.libs.json.{JsValue, Json}
import support.UnitSpec

class AmendBFLossResponseSpec extends UnitSpec {

  "Json Reads" should {
    val desJson: String => JsValue = lossType => Json.parse(s"""
        |{
        |  "incomeSourceId": "000000000000001",
        |  "lossType": "$lossType",
        |  "broughtForwardLossAmount": 99999999999.99,
        |  "taxYear": "2020"
        |}
      """.stripMargin)
    val desToModel: String => AmendBFLossResponse = typeOfLoss =>
      AmendBFLossResponse(selfEmploymentId = Some("000000000000001"), typeOfLoss = typeOfLoss, lossAmount = 99999999999.99, taxYear = "2019-20")
    val desToMtdMap: Map[String, String] = Map(
      "INCOME" -> "self-employment",
      "CLASS4" -> "self-employment-class4",
      "04"     -> "uk-fhl-property",
      "02"     -> "uk-other-property"
    )

    desToMtdMap.foreach {
      case (desType, mtdType) => {
        s"convert JSON from DES into a valid model for $desType" in {
          desJson(desType).as[AmendBFLossResponse] shouldBe desToModel(mtdType)
        }
      }
    }
  }
  "Json Writes" should {
    val model =
      AmendBFLossResponse(selfEmploymentId = Some("000000000000001"), typeOfLoss = "INCOME", lossAmount = 99999999999.99, taxYear = "2019-20")
    val mtdJson = Json.parse("""{
        |  "selfEmploymentId": "000000000000001",
        |  "typeOfLoss": "INCOME",
        |  "lossAmount": 99999999999.99,
        |  "taxYear": "2019-20"
        |}
      """.stripMargin)
    "convert a valid model into MTD JSON" in {
      Json.toJson(model) shouldBe mtdJson
    }
  }
}
