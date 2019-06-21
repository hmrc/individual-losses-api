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
import v1.models.des.DesBroughtForwardLoss
import v1.models.utils.JsonErrorValidators

class BroughtForwardLossBodySpec extends UnitSpec with JsonErrorValidators {

  val broughtForwardLoss = BroughtForwardLoss(typeOfLoss = "self-employment",
    selfEmploymentId = Some("XKIS00000000988"),
    taxYear = "2019-20",
    lossAmount = 256.78)


  val desbroughtForwardLoss = DesBroughtForwardLoss(lossType = "INCOME",
    incomeSourceId = Some("XKIS00000000988"),
    taxYearBroughtForwardFrom = "2020",
    broughtForwardLossAmount = 256.78)


  val broughtForwardLossJson = Json.parse(
    """
      |{
      |    "selfEmploymentId": "XKIS00000000988",
      |    "typeOfLoss": "self-employment",
      |    "taxYear": "2019-20",
      |    "lossAmount": 256.78
      |}
    """.stripMargin)

  "reads" when {
    "passed valid BroughtForwardLoss JSON" should {
      "return a valid model" in {
        broughtForwardLossJson.as[BroughtForwardLoss] shouldBe broughtForwardLoss
      }

      testMandatoryProperty[BroughtForwardLoss](broughtForwardLossJson)("/typeOfLoss")
      testPropertyType[BroughtForwardLoss](broughtForwardLossJson)(
        path = "/typeOfLoss",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[BroughtForwardLoss](broughtForwardLossJson)("/taxYear")
      testPropertyType[BroughtForwardLoss](broughtForwardLossJson)(
        path = "/taxYear",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[BroughtForwardLoss](broughtForwardLossJson)("/lossAmount")
      testPropertyType[BroughtForwardLoss](broughtForwardLossJson)(
        path = "/lossAmount",
        replacement = "dfgdf".toJson,
        expectedError = JsonError.NUMBER_FORMAT_EXCEPTION)
    }
  }

  "writes" when {
    "passed a valid BroughtForwardLoss model" should {
      "return a valid BroughtForwardLoss JSON" in {
        Json.toJson(broughtForwardLoss) shouldBe broughtForwardLossJson
      }
    }
  }


  "toDes" when {
    "passed valid BroughtForwardLoss" should {
      "return valid DesBroughtForwardLoss" in {

        val desResult = broughtForwardLoss.toDes(broughtForwardLoss)
        desResult shouldBe desbroughtForwardLoss

      }
    }
  }

  "Reading a typeOfLoss from Json" when {
    "the BFLoss model has a lossType of 'self-employment'" should {
      "create desBFLoss model with a lossType of 'INCOME'" in {
        val desResult = broughtForwardLoss.copy(typeOfLoss = "self-employment-class4").toDes(broughtForwardLoss)
        desResult shouldBe desbroughtForwardLoss.copy(lossType = "CLASS4")
      }
    }
    "the BFLoss model has a lossType of 'self-employment-class4'" should {
      "create desBFLoss model with a lossType of 'CLASS4'" in {
        val desResult = broughtForwardLoss.copy(typeOfLoss = "self-employment-class4").toDes(broughtForwardLoss)
        desResult shouldBe desbroughtForwardLoss.copy(lossType = "CLASS4")
      }
    }
    "the BFLoss model has a lossType of 'uk-fhl-property'" should {
      "create desBFLoss model with a lossType of '04'" in {
        val desResult = broughtForwardLoss.copy(typeOfLoss = "uk-fhl-property").toDes(broughtForwardLoss)
        desResult shouldBe desbroughtForwardLoss.copy(lossType = "04")
      }
    }
    "the BFLoss model has a lossType of 'uk-other-property'" should {
      "create desBFLoss model with a lossType of '02'" in {
        val desResult = broughtForwardLoss.copy(typeOfLoss = "uk-other-property").toDes(broughtForwardLoss)
        desResult shouldBe desbroughtForwardLoss.copy(lossType = "02")
      }
    }
  }
}