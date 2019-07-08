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
import v1.models.domain.TypeOfLoss

class AmendBFLossResponseSpec extends UnitSpec {

  "Json Reads" should {
    def desPropertyJson(incomeSourceType: String): JsValue = {
      Json.parse(s"""
           |{
           |  "incomeSourceId": "000000000000001",
           |  "incomeSourceType": "$incomeSourceType",
           |  "broughtForwardLossAmount": 99999999999.99,
           |  "taxYear": "2020"
           |}
      """.stripMargin)
    }

    def desEmploymentJson(lossType: String): JsValue = {
      Json.parse(s"""
           |{
           |  "incomeSourceId": "000000000000001",
           |  "lossType": "$lossType",
           |  "broughtForwardLossAmount": 99999999999.99,
           |  "taxYear": "2020"
           |}
      """.stripMargin)
    }

    def desToModel: TypeOfLoss => AmendBFLossResponse =
      typeOfLoss =>
        AmendBFLossResponse(selfEmploymentId = Some("000000000000001"), typeOfLoss = typeOfLoss, lossAmount = 99999999999.99, taxYear = "2019-20")

    "convert property JSON from DES into a valid model for property type 02" in {
      desPropertyJson("02").as[AmendBFLossResponse] shouldBe desToModel(TypeOfLoss.`uk-property-non-fhl`)
    }

    "convert property JSON from DES into a valid model for property type 04" in {
      desPropertyJson("04").as[AmendBFLossResponse] shouldBe desToModel(TypeOfLoss.`uk-property-fhl`)
    }

    "convert employment JSON from DES into a valid model for property type INCOME" in {
      desEmploymentJson("INCOME").as[AmendBFLossResponse] shouldBe desToModel(TypeOfLoss.`self-employment`)
    }

    "convert employment JSON from DES into a valid model for property type CLASS4" in {
      desEmploymentJson("CLASS4").as[AmendBFLossResponse] shouldBe desToModel(TypeOfLoss.`self-employment-class4`)
    }
  }
  "Json Writes" should {
    val model =
      AmendBFLossResponse(selfEmploymentId = Some("000000000000001"),
                          typeOfLoss = TypeOfLoss.`self-employment`,
                          lossAmount = 99999999999.99,
                          taxYear = "2019-20")
    val mtdJson = Json.parse("""{
        |  "selfEmploymentId": "000000000000001",
        |  "typeOfLoss": "self-employment",
        |  "lossAmount": 99999999999.99,
        |  "taxYear": "2019-20"
        |}
      """.stripMargin)
    "convert a valid model into MTD JSON" in {
      Json.toJson(model) shouldBe mtdJson
    }
  }
}
