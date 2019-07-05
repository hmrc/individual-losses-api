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

import java.time.LocalDateTime

import play.api.libs.json._
import support.UnitSpec
import v1.models.domain.TypeOfLoss

class RetrieveBFLossResponseSpec extends UnitSpec {

  val testDateTime: LocalDateTime = LocalDateTime.now()

  val validPropertyJson: JsValue = Json.parse(s"""
      |{
      | "lossId" : "12356789abcdefg",
      | "taxYear" : "2018",
      | "incomeSourceType" : "04",
      | "broughtForwardLossAmount" : 1000.25,
      | "submissionDate" : "${testDateTime.toString}"
      |}
    """.stripMargin)

  val validSEJson: JsValue = Json.parse(s"""
       |{
       | "lossId" : "12356789abcdefg",
       | "taxYear" : "2019",
       | "lossType" : "INCOME",
       | "incomeSourceId" : "incomeSourceId",
       | "broughtForwardLossAmount" : 250.55,
       | "submissionDate" : "${testDateTime.toString}"
       |}
    """.stripMargin)

  "Json Reads" should {

    "return a validated model" when {

      "provided with valid property loss data" in {
        val result = validPropertyJson.validate[RetrieveBFLossResponse]

        println(result)

        result.isSuccess shouldBe true
        result.get shouldBe RetrieveBFLossResponse("2017-18", TypeOfLoss.`uk-property-fhl`, None, 1000.25, testDateTime.toString)
      }

      "provided with valid self-employed loss data" in {
        val result = validSEJson.validate[RetrieveBFLossResponse]

        result.isSuccess shouldBe true
        result.get shouldBe RetrieveBFLossResponse("2018-19", TypeOfLoss.`self-employment`, Some("incomeSourceId"), 250.55, testDateTime.toString)
      }
    }

    "return a failed model" when {

      val propertyRequiredElements = Seq("taxYear", "incomeSourceType", "broughtForwardLossAmount", "submissionDate")
      val seRequiredElements       = Seq("taxYear", "lossType", "broughtForwardLossAmount", "submissionDate")

      propertyRequiredElements.foreach { element =>
        s"the required element '$element' is missing from property data type" in {
          val invalidJson = validPropertyJson.as[JsObject] - element
          val result      = invalidJson.validate[RetrieveBFLossResponse]

          result.isError shouldBe true
        }
      }

      seRequiredElements.foreach { element =>
        s"the required element '$element' is missing from se data type" in {
          val invalidJson = validSEJson.as[JsObject] - element
          val result      = invalidJson.validate[RetrieveBFLossResponse]

          result.isError shouldBe true
        }
      }
    }
  }

  "Json writes" should {

    "successfully produce json" when {

      "provided with a property model" in {
        val model = RetrieveBFLossResponse("2017-18", TypeOfLoss.`uk-property-fhl`, None, 2500.55, testDateTime.toString)

        val expectedPropertyJson: JsValue = Json.parse(s"""
            |{
            | "taxYear" : "2017-18",
            | "typeOfLoss" : "uk-property-fhl",
            | "lossAmount" : 2500.55,
            | "lastModified" : "${testDateTime.toString}"
            |}
          """.stripMargin)

        Json.toJson(model) shouldBe expectedPropertyJson
      }

      "provided with a se model" in {
        val model = RetrieveBFLossResponse("2018-19", TypeOfLoss.`self-employment`, Some("someId"), 2500.56, testDateTime.toString)

        val expectedSeJson: JsValue = Json.parse(s"""
             |{
             | "taxYear" : "2018-19",
             | "typeOfLoss" : "self-employment",
             | "selfEmploymentId" : "someId",
             | "lossAmount" : 2500.56,
             | "lastModified" : "${testDateTime.toString}"
             |}
          """.stripMargin)

        Json.toJson(model) shouldBe expectedSeJson
      }
    }
  }
}
