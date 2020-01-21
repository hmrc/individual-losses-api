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

import play.api.libs.json._
import support.UnitSpec
import v1.models.utils.JsonErrorValidators

class BFLossBodySpec extends UnitSpec with JsonErrorValidators {

  val broughtForwardLossEmployment =
    BFLoss(typeOfLoss = TypeOfLoss.`self-employment`, selfEmploymentId = Some("XKIS00000000988"), taxYear = "2019-20", lossAmount = 256.78)

  val broughtForwardLossProperty =
    BFLoss(typeOfLoss = TypeOfLoss.`uk-property-fhl`, selfEmploymentId = None, taxYear = "2019-20", lossAmount = 255.50)

  val broughtForwardLossEmploymentJson: JsValue = Json.parse("""
      |{
      |    "selfEmploymentId": "XKIS00000000988",
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

  val broughtForwardLossPropertyJson: JsValue = Json.parse("""
      |{
      |	  "typeOfLoss": "uk-property-fhl",
      |	  "taxYear": "2019-20",
      |	  "lossAmount": 255.50
      |}
    """.stripMargin)

  val broughtForwardLossPropertyDesJson: JsValue = Json.parse("""
      |{
      |	  "incomeSourceType":"04",
      |	  "taxYear": "2020",
      |	  "broughtForwardLossAmount": 255.50
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
        BFLoss.writes.writes(broughtForwardLossEmployment) shouldBe broughtForwardLossEmploymentDesJson
      }
    }
    "passed a valid BroughtForwardLoss Property model" should {
      "return a valid BroughtForwardLoss Property JSON" in {
        BFLoss.writes.writes(broughtForwardLossProperty) shouldBe broughtForwardLossPropertyDesJson
      }
    }
  }
}
