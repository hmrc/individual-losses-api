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
import v1.models.domain.{TypeOfClaim, TypeOfLoss}

class RetrieveLossClaimResponseSpec extends UnitSpec {

  val testDateTime: LocalDateTime = LocalDateTime.now()

  private val validTaxYear          = "2019-20"
  private val validSelfEmploymentId = "XAIS01234567890"

  val employmentModel = RetrieveLossClaimResponse(
    validTaxYear,
    TypeOfLoss.`self-employment`,
    Some(validSelfEmploymentId),
    TypeOfClaim.`carry-sideways`,
    testDateTime.toString
  )

  val propertyModel = RetrieveLossClaimResponse(
    validTaxYear,
    TypeOfLoss.`uk-property-non-fhl`,
    None,
    TypeOfClaim.`carry-forward`,
    testDateTime.toString
  )


  val validSEJson: JsValue = Json.parse(s"""
                                           |{
                                           | "incomeSourceId" : "XAIS01234567890",
                                           | "reliefClaimed" : "CSGI",
                                           | "taxYearClaimedFor" : "2020",
                                           | "submissionDate" : "${testDateTime.toString}"
                                           |}
    """.stripMargin)

  val validPropertyJson: JsValue = Json.parse(s"""
                                                 |{
                                                 | "incomeSourceType" : "02",
                                                 | "reliefClaimed" : "CF",
                                                 | "taxYearClaimedFor" : "2020",
                                                 | "submissionDate" : "${testDateTime.toString}"
                                                 |}
    """.stripMargin)

  "Json Reads" should {

    "return a validated model" when {

      "provided with valid self-employment loss data" in {

        val result = validSEJson.validate[RetrieveLossClaimResponse]
        result.isSuccess shouldBe true
        result.get shouldBe employmentModel

      }

      "provided with valid property loss data" in {
        val result = validPropertyJson.validate[RetrieveLossClaimResponse]

        result.isSuccess shouldBe true
        result.get shouldBe propertyModel
      }
    }


    "return a failed model" when {

      val propertyRequiredElements = Seq("taxYearClaimedFor", "reliefClaimed", "submissionDate")
      val seRequiredElements = Seq("taxYearClaimedFor", "reliefClaimed", "submissionDate")

      propertyRequiredElements.foreach { element =>
        s"the required element '$element' is missing from property data type" in {
          val invalidJson = validPropertyJson.as[JsObject] - element
          val result = invalidJson.validate[RetrieveLossClaimResponse]

          result.isError shouldBe true
        }
      }

      seRequiredElements.foreach { element =>
        s"the required element '$element' is missing from se data type" in {
          val invalidJson = validSEJson.as[JsObject] - element
          val result = invalidJson.validate[RetrieveLossClaimResponse]

          result.isError shouldBe true
        }
      }
    }

    "Json writes" should {

      "successfully produce json" when {

        "provided a carry-forward property model" in {

          val expectedPropertyJson: JsValue = Json.parse(
            s"""
               |{
               | "typeOfLoss": "uk-property-non-fhl",
               | "typeOfClaim": "carry-forward",
               | "taxYear": "2019-20",
               | "lastModified" : "${testDateTime.toString}"
               |}
            """.stripMargin)

          Json.toJson(propertyModel) shouldBe expectedPropertyJson
        }

        "provided a carry-forward-to-carry-sideways-general-income property model" in {

          val expectedPropertyJson: JsValue = Json.parse(
            s"""
               |{
               | "typeOfLoss": "uk-property-non-fhl",
               | "typeOfClaim": "carry-forward-to-carry-sideways-general-income",
               | "taxYear": "2019-20",
               | "lastModified" : "${testDateTime.toString}"
               |}
            """.stripMargin)

          Json.toJson(propertyModel.copy(typeOfClaim = TypeOfClaim.`carry-forward-to-carry-sideways-general-income`)) shouldBe expectedPropertyJson
        }

        "provided a carry-sideways-fhl property model" in {

          val expectedPropertyJson: JsValue = Json.parse(
            s"""
               |{
               | "typeOfLoss": "uk-property-non-fhl",
               | "typeOfClaim": "carry-sideways-fhl",
               | "taxYear": "2019-20",
               | "lastModified" : "${testDateTime.toString}"
               |}
            """.stripMargin)

          Json.toJson(propertyModel.copy(typeOfClaim = TypeOfClaim.`carry-sideways-fhl`)) shouldBe expectedPropertyJson
        }

        "provided with a se model" in {

          val expectedSEJson: JsValue = Json.parse(
            s"""
               |{
               | "typeOfLoss": "self-employment",
               |  "selfEmploymentId": "XAIS01234567890",
               | "typeOfClaim": "carry-sideways",
               | "taxYear": "2019-20",
               | "lastModified" : "${testDateTime.toString}"
               |}
            """.stripMargin)

          Json.toJson(employmentModel) shouldBe expectedSEJson
        }
      }
    }
  }
}
