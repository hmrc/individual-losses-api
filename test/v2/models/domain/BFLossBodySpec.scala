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

package v2.models.domain

import play.api.libs.json._
import support.UnitSpec
import v2.models.utils.JsonErrorValidators

class BFLossBodySpec extends UnitSpec with JsonErrorValidators {

  val broughtForwardLossEmployment =
    BFLoss(typeOfLoss = TypeOfLoss.`self-employment`, businessId = Some("XKIS00000000988"), taxYear = "2019-20", lossAmount = 256.78)

  val broughtForwardLossForeignProperty =
    BFLoss(typeOfLoss = TypeOfLoss.`foreign-property`, businessId = Some("XKIS00000000988"), taxYear = "2019-20", lossAmount = 256.78)

  val broughtForwardLossEmploymentJson: JsValue = Json.parse("""
      |{
      |    "businessId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin)

  val broughtForwardLossEmploymentDesJson: JsValue = Json.parse("""
      |{
      |	  "incomeSourceId": "XKIS00000000988",
      |	  "lossType": "INCOME",
      |	  "taxYear": "2020",
      |	  "broughtForwardLossAmount": 256.78
      |}
    """.stripMargin)

  val broughtForwardLossForeignPropertyJson: JsValue = Json.parse("""
      |{
      |  "businessId": "XKIS00000000988",
      |  "typeOfLoss": "foreign-property",
      |  "taxYear": "2019-20",
      |  "lossAmount": 256.78
      |}
    """.stripMargin)

  val broughtForwardLossForeignPropertyDesJson: JsValue = Json.parse("""
      |{
      |	  "incomeSourceId": "XKIS00000000988",
      |	  "incomeSourceType": "15",
      |	  "taxYear": "2020",
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

      testMandatoryProperty[BFLoss](broughtForwardLossEmploymentJson)("/taxYear")
      testPropertyType[BFLoss](broughtForwardLossEmploymentJson)(path = "/taxYear",
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
      "return a valid BroughtForwardLoss Employment JSON" in {
        Json.toJson(broughtForwardLossEmployment) shouldBe broughtForwardLossEmploymentDesJson
      }
    }
    "passed a valid BroughtForwardLoss UK Property model" should {
      Seq(TypeOfLoss.`uk-property-fhl`, TypeOfLoss.`uk-property-non-fhl`).foreach(
        typeOfLoss =>
          s"return valid JSON without a businessId for $typeOfLoss" in {
            val model         = BFLoss(typeOfLoss = typeOfLoss, businessId = Some("XKIS00000000988"), taxYear = "2019-20", lossAmount = 255.50)
            val json: JsValue = Json.parse(s"""{
                                              |	  "incomeSourceType":"${typeOfLoss.toIncomeSourceType.get}",
                                              |	  "taxYear": "2020",
                                              |	  "broughtForwardLossAmount": 255.50
                                              |}""".stripMargin)
            Json.toJson(model) shouldBe json
          }
      )
    }
    "passed a valid BroughtForwardLoss Foreign Property model" should {
      "return a valid BroughtForwardLoss Foreign Property JSON" in {
        Json.toJson(broughtForwardLossForeignProperty) shouldBe broughtForwardLossForeignPropertyDesJson
      }
    }
  }
}
