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

package v1.models.domain

import play.api.libs.json._
import support.UnitSpec
import v1.models.utils.JsonErrorValidators

class BFLossBodySpec extends UnitSpec with JsonErrorValidators {

  val broughtForwardLossEmployment = BFLoss(
    typeOfLoss = "self-employment",
    selfEmploymentId = Some("XKIS00000000988"),
    taxYear = "2019-20",
    lossAmount = 256.78)

  val broughtForwardLossProperty = BFLoss(
    typeOfLoss = "uk-fhl-property",
    selfEmploymentId = None,
    taxYear = "2019-20",
    lossAmount = 255.50)


  val broughtForwardLossEmploymentJson = Json.parse(
    """
      |{
      |    "selfEmploymentId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin)

  val broughtForwardLossEmploymentDesJson = Json.parse(
    """
      |{
      |	  "incomeSourceId": "XKIS00000000988",
      |	  "lossType": "INCOME",
      |	  "taxYear": "2020",
      |	  "broughtForwardLossAmount": 256.78
      |}
    """.stripMargin)

  val broughtForwardLossPropertyJson = Json.parse(
    """
      |{
      |	  "typeOfLoss": "uk-fhl-property",
      |	  "taxYear": "2019-20",
      |	  "lossAmount": 255.50
      |}
    """.stripMargin)

  val broughtForwardLossPropertyDesJson = Json.parse(
    """
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
      testPropertyType[BFLoss](broughtForwardLossEmploymentJson)(
        path = "/typeOfLoss",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[BFLoss](broughtForwardLossEmploymentJson)("/taxYear")
      testPropertyType[BFLoss](broughtForwardLossEmploymentJson)(
        path = "/taxYear",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[BFLoss](broughtForwardLossEmploymentJson)("/lossAmount")
      testPropertyType[BFLoss](broughtForwardLossEmploymentJson)(
        path = "/lossAmount",
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

  "Reading a typeOfLoss from Json" when {
    "the BFLoss model has a lossType of 'self-employment'" should {
      "create BFLoss model with a lossType of 'INCOME'" in {
        BFLoss.convertToDesCode("self-employment") shouldBe "INCOME"
      }
    }
    "the BFLoss model has a lossType of 'self-employment-class4'" should {
      "create BFLoss model with a lossType of 'CLASS4'" in {
        BFLoss.convertToDesCode("self-employment-class4") shouldBe "CLASS4"
      }
    }
    "the BFLoss model has a lossType of 'uk-fhl-property'" should {
      "create BFLoss model with a lossType of '04'" in {
        BFLoss.convertToDesCode("uk-fhl-property") shouldBe "04"
      }
    }
    "the BFLoss model has a lossType of 'uk-other-property'" should {
      "create BFLoss model with a lossType of '02'" in {
        BFLoss.convertToDesCode("uk-other-property") shouldBe "02"
      }
    }
  }
}