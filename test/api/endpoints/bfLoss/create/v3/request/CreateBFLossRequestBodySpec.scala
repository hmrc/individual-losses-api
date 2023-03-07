/*
 * Copyright 2023 HM Revenue & Customs
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

package api.endpoints.bfLoss.create.v3.request

import api.endpoints.bfLoss.domain.anyVersion.TypeOfLoss
import api.models.utils.JsonErrorValidators
import play.api.libs.json._
import support.UnitSpec

class CreateBFLossRequestBodySpec extends UnitSpec with JsonErrorValidators {

  val broughtForwardLossEmployment: CreateBFLossRequestBody =
    CreateBFLossRequestBody(typeOfLoss = TypeOfLoss.`self-employment`,
                            businessId = "XKIS00000000988",
                            taxYearBroughtForwardFrom = "2019-20",
                            lossAmount = 256.78)

  val broughtForwardLossForeignProperty: CreateBFLossRequestBody =
    CreateBFLossRequestBody(typeOfLoss = TypeOfLoss.`foreign-property`,
                            businessId = "XKIS00000000988",
                            taxYearBroughtForwardFrom = "2019-20",
                            lossAmount = 256.78)

  val broughtForwardLossEmploymentJson: JsValue = Json.parse("""
      |{
      |    "businessId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYearBroughtForwardFrom": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin)

  val broughtForwardLossEmploymentDownstreamJson: JsValue = Json.parse("""
      |{
      |	  "incomeSourceId": "XKIS00000000988",
      |	  "lossType": "INCOME",
      |	  "taxYearBroughtForwardFrom": 2020,
      |	  "broughtForwardLossAmount": 256.78
      |}
    """.stripMargin)

  val broughtForwardLossForeignPropertyJson: JsValue = Json.parse("""
      |{
      |  "businessId": "XKIS00000000988",
      |  "typeOfLoss": "foreign-property",
      |  "taxYearBroughtForwardFrom": "2019-20",
      |  "lossAmount": 256.78
      |}
    """.stripMargin)

  val broughtForwardLossForeignPropertyDownstreamJson: JsValue = Json.parse("""
      |{
      |	  "incomeSourceId": "XKIS00000000988",
      |	  "incomeSourceType": "15",
      |	  "taxYearBroughtForwardFrom": 2020,
      |	  "broughtForwardLossAmount": 256.78
      |}
    """.stripMargin)

  "reads" when {
    "passed valid BroughtForwardLoss JSON" should {
      "return a valid model" in {
        broughtForwardLossEmploymentJson.as[CreateBFLossRequestBody] shouldBe broughtForwardLossEmployment
      }

      testMandatoryProperty[CreateBFLossRequestBody](broughtForwardLossEmploymentJson)("/typeOfLoss")
      testPropertyType[CreateBFLossRequestBody](broughtForwardLossEmploymentJson)(path = "/typeOfLoss",
                                                                                  replacement = 12344.toJson,
                                                                                  expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[CreateBFLossRequestBody](broughtForwardLossEmploymentJson)("/taxYearBroughtForwardFrom")
      testPropertyType[CreateBFLossRequestBody](broughtForwardLossEmploymentJson)(path = "/taxYearBroughtForwardFrom",
                                                                                  replacement = 12344.toJson,
                                                                                  expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[CreateBFLossRequestBody](broughtForwardLossEmploymentJson)("/lossAmount")
      testPropertyType[CreateBFLossRequestBody](broughtForwardLossEmploymentJson)(path = "/lossAmount",
                                                                                  replacement = "dfgdf".toJson,
                                                                                  expectedError = JsonError.NUMBER_FORMAT_EXCEPTION)
    }
  }

  "writes" when {
    "writing a BroughtForwardLoss Employment instance" should {
      Seq(TypeOfLoss.`self-employment`, TypeOfLoss.`self-employment-class4`).foreach(
        typeOfLoss =>
          s"return valid JSON with incomeSourceType set correctly for $typeOfLoss" in {
            val requestObject = CreateBFLossRequestBody(typeOfLoss = typeOfLoss,
                                                        businessId = "XKIS00000000988",
                                                        taxYearBroughtForwardFrom = "2019-20",
                                                        lossAmount = 255.50)
            val expectedJson: JsValue = Json.parse(s"""
               |{
               |   "incomeSourceId": "XKIS00000000988",
               |	  "lossType":"${typeOfLoss.toLossType.get}",
               |	  "taxYearBroughtForwardFrom": 2020,
               |	  "broughtForwardLossAmount": 255.50
               |}""".stripMargin)
            Json.toJson(requestObject) shouldBe expectedJson
        }
      )
    }
    "passed a valid BroughtForwardLoss UK Property model" should {
      Seq(TypeOfLoss.`uk-property-fhl`, TypeOfLoss.`uk-property-non-fhl`).foreach(
        typeOfLoss =>
          s"return valid JSON with incomeSourceType set correctly for $typeOfLoss" in {
            val model = CreateBFLossRequestBody(typeOfLoss = typeOfLoss,
                                                businessId = "XKIS00000000988",
                                                taxYearBroughtForwardFrom = "2019-20",
                                                lossAmount = 255.50)
            val json: JsValue = Json.parse(s"""{
                                              |   "incomeSourceId": "XKIS00000000988",
                                              |	  "incomeSourceType":"${typeOfLoss.toIncomeSourceType.get}",
                                              |	  "taxYearBroughtForwardFrom": 2020,
                                              |	  "broughtForwardLossAmount": 255.50
                                              |}""".stripMargin)
            Json.toJson(model) shouldBe json
        }
      )
    }
    "passed a valid BroughtForwardLoss Foreign Property model" should {
      Seq(TypeOfLoss.`foreign-property-fhl-eea`, TypeOfLoss.`foreign-property`).foreach(
        typeOfLoss =>
          s"return valid JSON with incomeSourceType for $typeOfLoss" in {
            val model = CreateBFLossRequestBody(typeOfLoss = typeOfLoss,
                                                businessId = "XKIS00000000988",
                                                taxYearBroughtForwardFrom = "2019-20",
                                                lossAmount = 255.50)
            val json: JsValue = Json.parse(s"""
              |{
              |   "incomeSourceId": "XKIS00000000988",
              |	  "incomeSourceType":"${typeOfLoss.toIncomeSourceType.get}",
              |	  "taxYearBroughtForwardFrom": 2020,
              |	  "broughtForwardLossAmount": 255.50
              |}""".stripMargin)
            Json.toJson(model) shouldBe json
        }
      )
    }
  }
}
