/*
 * Copyright 2022 HM Revenue & Customs
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

package v3.models.domain

import play.api.libs.json._
import support.UnitSpec
import v3.models.utils.JsonErrorValidators
import BFLossTypeOfLoss._

class BFLossSpec extends UnitSpec with JsonErrorValidators {

  val broughtForwardLossEmployment: BFLoss =
    BFLoss(typeOfLoss = BFLossTypeOfLoss.`self-employment`, businessId = "XKIS00000000988", taxYearBroughtForwardFrom = "2019-20", lossAmount = 256.78)

  val broughtForwardLossForeignProperty: BFLoss =
    BFLoss(typeOfLoss = BFLossTypeOfLoss.`foreign-property`, businessId = "XKIS00000000988", taxYearBroughtForwardFrom = "2019-20", lossAmount = 256.78)

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

  val broughtForwardLossForeignPropertyDowwnstreamJson: JsValue = Json.parse("""
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
        broughtForwardLossEmploymentJson.as[BFLoss] shouldBe broughtForwardLossEmployment
      }

      testMandatoryProperty[BFLoss](broughtForwardLossEmploymentJson)("/typeOfLoss")
      testPropertyType[BFLoss](broughtForwardLossEmploymentJson)(path = "/typeOfLoss",
                                                                 replacement = 12344.toJson,
                                                                 expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[BFLoss](broughtForwardLossEmploymentJson)("/taxYearBroughtForwardFrom")
      testPropertyType[BFLoss](broughtForwardLossEmploymentJson)(path = "/taxYearBroughtForwardFrom",
                                                                 replacement = 12344.toJson,
                                                                 expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[BFLoss](broughtForwardLossEmploymentJson)("/lossAmount")
      testPropertyType[BFLoss](broughtForwardLossEmploymentJson)(path = "/lossAmount",
                                                                 replacement = "dfgdf".toJson,
                                                                 expectedError = JsonError.NUMBER_FORMAT_EXCEPTION)
    }
  }

  "writes" when {
    "passed a valid BroughtForwardLoss Employment model" should {
      Seq(`self-employment`, `self-employment-class4`).foreach(
        typeOfLoss =>
          s"return valid JSON with incomeSourceType set correctly for $typeOfLoss" in {
            val model         = BFLoss(typeOfLoss = typeOfLoss, businessId = "XKIS00000000988", taxYearBroughtForwardFrom = "2019-20", lossAmount = 255.50)
            val json: JsValue = Json.parse(s"""{
                                              |   "incomeSourceId": "XKIS00000000988",
                                              |	  "lossType":"${typeOfLoss.toLossType}",
                                              |	  "taxYearBroughtForwardFrom": 2020,
                                              |	  "broughtForwardLossAmount": 255.50
                                              |}""".stripMargin)
            Json.toJson(model) shouldBe json
          }
      )
    }
    "passed a valid BroughtForwardLoss UK Property model" should {
      Seq(`uk-property-fhl`, `uk-property-non-fhl`).foreach(
        typeOfLoss =>
          s"return valid JSON with incomeSourceType set correctly for $typeOfLoss" in {
            val model         = BFLoss(typeOfLoss = typeOfLoss, businessId = "XKIS00000000988", taxYearBroughtForwardFrom = "2019-20", lossAmount = 255.50)
            val json: JsValue = Json.parse(s"""{
                                              |   "incomeSourceId": "XKIS00000000988",
                                              |	  "incomeSourceType":"${typeOfLoss.toIncomeSourceType}",
                                              |	  "taxYearBroughtForwardFrom": 2020,
                                              |	  "broughtForwardLossAmount": 255.50
                                              |}""".stripMargin)
            Json.toJson(model) shouldBe json
          }
      )
    }
    "passed a valid BroughtForwardLoss Foreign Property model" should {
      Seq(`foreign-property-fhl-eea`, `foreign-property`).foreach(
        typeOfLoss =>
          s"return valid JSON with incomeSourceType set correctly for $typeOfLoss" in {
            val model         = BFLoss(typeOfLoss = typeOfLoss, businessId = "XKIS00000000988", taxYearBroughtForwardFrom = "2019-20", lossAmount = 255.50)
            val json: JsValue = Json.parse(s"""{
                                              |   "incomeSourceId": "XKIS00000000988",
                                              |	  "incomeSourceType":"${typeOfLoss.toIncomeSourceType}",
                                              |	  "taxYearBroughtForwardFrom": 2020,
                                              |	  "broughtForwardLossAmount": 255.50
                                              |}""".stripMargin)
            Json.toJson(model) shouldBe json
          }
      )
    }
  }
}
