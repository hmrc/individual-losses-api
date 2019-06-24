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

import play.api.libs.json._
import support.UnitSpec
import v1.models.domain.BFLoss
import v1.models.utils.JsonErrorValidators

class DesBFLossBodySpec extends UnitSpec with JsonErrorValidators {

  val broughtForwardLoss = BFLoss(typeOfLoss = "self-employment",
    selfEmploymentId = Some("XKIS00000000988"),
    taxYear = "2019-20",
    lossAmount = 256.78)



  val desBroughtForwardLoss = DesBFLoss(lossType = "INCOME",
    incomeSourceId = Some("XKIS00000000988"),
    taxYearBroughtForwardFrom = "2020",
    broughtForwardLossAmount = 256.78)


  val desBroughtForwardLossJson = Json.parse(
    """
      |{
      |    "incomeSourceId": "XKIS00000000988",
      |    "lossType": "INCOME",
      |    "taxYearBroughtForwardFrom": "2020",
      |    "broughtForwardLossAmount": 256.78
      |}
    """.stripMargin)

  "reads" when {
    "passed valid DesBroughtForwardLoss JSON" should {
      "return a valid model" in {
        desBroughtForwardLossJson.as[DesBFLoss] shouldBe desBroughtForwardLoss
      }

      testMandatoryProperty[DesBFLoss](desBroughtForwardLossJson)("/lossType")
      testPropertyType[DesBFLoss](desBroughtForwardLossJson)(
        path = "/lossType",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[DesBFLoss](desBroughtForwardLossJson)("/taxYearBroughtForwardFrom")
      testPropertyType[DesBFLoss](desBroughtForwardLossJson)(
        path = "/taxYearBroughtForwardFrom",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[DesBFLoss](desBroughtForwardLossJson)("/broughtForwardLossAmount")
      testPropertyType[DesBFLoss](desBroughtForwardLossJson)(
        path = "/broughtForwardLossAmount",
        replacement = "dfgdf".toJson,
        expectedError = JsonError.NUMBER_FORMAT_EXCEPTION)
    }
  }

  "writes" when {
    "passed a valid BroughtForwardLoss model" should {
      "return a valid BroughtForwardLoss JSON" in {
        Json.toJson(desBroughtForwardLoss) shouldBe desBroughtForwardLossJson
      }
    }
  }


  "toMtd" when {
    "passed valid DesBroughtForwardLoss" should {
      "return valid BroughtForwardLoss" in {

        val desResult = desBroughtForwardLoss.toDes(desBroughtForwardLoss)
        desResult shouldBe broughtForwardLoss

      }
    }
  }

  "Reading a typeOfLoss from Json" when {
    "the desBFLoss model has a lossType of 'INCOME'" should {
      "create BFLoss model with a lossType of 'self-employment'" in {
        val desResult = desBroughtForwardLoss.copy(lossType = "INCOME").toDes(desBroughtForwardLoss)
        desResult shouldBe broughtForwardLoss.copy(typeOfLoss = "self-employment")
      }
    }
    "the desBFLoss model has a lossType of 'CLASS4'" should {
      "create BFLoss model with a lossType of 'self-employment-class4'" in {
        val desResult = desBroughtForwardLoss.copy(lossType = "CLASS4").toDes(desBroughtForwardLoss)
        desResult shouldBe broughtForwardLoss.copy(typeOfLoss = "self-employment-class4")
      }
    }
    "the desBFLoss model has a lossType of '04'" should {
      "create BFLoss model with a lossType of 'uk-fhl-property'" in {
        val desResult = desBroughtForwardLoss.copy(lossType = "04").toDes(desBroughtForwardLoss)
        desResult shouldBe broughtForwardLoss.copy(typeOfLoss = "uk-fhl-property")
      }
    }
    "the desBFLoss model has a lossType of '02'" should {
      "create BFLoss model with a lossType of 'uk-other-property'" in {
        val desResult = desBroughtForwardLoss.copy(lossType = "02").toDes(desBroughtForwardLoss)
        desResult shouldBe broughtForwardLoss.copy(typeOfLoss = "uk-other-property")
      }
    }
  }
}