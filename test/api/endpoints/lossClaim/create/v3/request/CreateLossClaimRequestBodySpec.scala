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

package api.endpoints.lossClaim.create.v3.request

import api.endpoints.lossClaim.create.v3.request
import api.endpoints.lossClaim.domain.v3.{TypeOfClaim, TypeOfLoss}
import api.models.utils.JsonErrorValidators
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec

class CreateLossClaimRequestBodySpec extends UnitSpec with JsonErrorValidators {

  val lossClaimSelfEmployment: CreateLossClaimRequestBody = request.CreateLossClaimRequestBody(
    taxYearClaimedFor = "2019-20",
    typeOfLoss = TypeOfLoss.`self-employment`,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    businessId = "X2IS12356589871"
  )

  val lossClaimSelfEmploymentJson: JsValue = Json.parse("""
      |{
      |    "typeOfLoss": "self-employment",
      |    "businessId": "X2IS12356589871",
      |    "typeOfClaim": "carry-forward",
      |    "taxYearClaimedFor": "2019-20"
      |}
    """.stripMargin)

  val lossClaimSelfEmploymentDownstreamJson: JsValue = Json.parse("""
      |{
      |    "incomeSourceId": "X2IS12356589871",
      |    "reliefClaimed": "CF",
      |    "taxYear": "2020"
      |}
    """.stripMargin)

  val lossClaimUkPropertyNonFhl: CreateLossClaimRequestBody = request.CreateLossClaimRequestBody(
    taxYearClaimedFor = "2019-20",
    typeOfLoss = TypeOfLoss.`uk-property-non-fhl`,
    typeOfClaim = TypeOfClaim.`carry-forward-to-carry-sideways`,
    businessId = "X2IS12356589871"
  )

  val lossClaimUkPropertyNonFhlJson: JsValue = Json.parse("""
      |{
      |    "businessId": "X2IS12356589871",
      |    "typeOfLoss": "uk-property-non-fhl",
      |    "typeOfClaim": "carry-forward-to-carry-sideways",
      |    "taxYearClaimedFor": "2019-20"
      |}
    """.stripMargin)

  val lossClaimUkPropertyNonFhlDownstreamJson: JsValue = Json.parse("""
      |{
      |    "incomeSourceId": "X2IS12356589871",
      |    "incomeSourceType": "02",
      |    "reliefClaimed": "CFCSGI",
      |    "taxYear": "2020"
      |}
    """.stripMargin)

  val lossClaimForeignProperty: CreateLossClaimRequestBody = request.CreateLossClaimRequestBody(
    taxYearClaimedFor = "2019-20",
    typeOfLoss = TypeOfLoss.`foreign-property`,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    businessId = "X2IS12356589871"
  )

  val lossClaimForeignPropertyJson: JsValue = Json.parse("""
      |{
      |    "typeOfLoss": "foreign-property",
      |    "businessId": "X2IS12356589871",
      |    "typeOfClaim": "carry-forward",
      |    "taxYearClaimedFor": "2019-20"
      |}
    """.stripMargin)

  val lossClaimForeignPropertyDowwnstreamJson: JsValue = Json.parse("""
      |{
      |    "incomeSourceId": "X2IS12356589871",
      |    "incomeSourceType": "15",
      |    "reliefClaimed": "CF",
      |    "taxYear": "2020"
      |}
    """.stripMargin)

  "reads" when {
    "passed a valid LossClaim Json" should {
      "return a valid model" when {
        Map[CreateLossClaimRequestBody, JsValue](
          lossClaimSelfEmployment   -> lossClaimSelfEmploymentJson,
          lossClaimUkPropertyNonFhl -> lossClaimUkPropertyNonFhlJson,
          lossClaimForeignProperty  -> lossClaimForeignPropertyJson,
        ).foreach {
          case (model, json) =>
            s"typeOfLoss = ${model.typeOfLoss}" in {
              json.as[CreateLossClaimRequestBody] shouldBe model
            }
        }
      }

      testMandatoryProperty[CreateLossClaimRequestBody](lossClaimSelfEmploymentJson)("/typeOfLoss")
      testPropertyType[CreateLossClaimRequestBody](lossClaimSelfEmploymentJson)(path = "/typeOfLoss",
                                                                                replacement = 12344.toJson,
                                                                                expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[CreateLossClaimRequestBody](lossClaimSelfEmploymentJson)("/taxYearClaimedFor")
      testPropertyType[CreateLossClaimRequestBody](lossClaimSelfEmploymentJson)(path = "/taxYearClaimedFor",
                                                                                replacement = 12344.toJson,
                                                                                expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[CreateLossClaimRequestBody](lossClaimSelfEmploymentJson)("/typeOfClaim")
      testPropertyType[CreateLossClaimRequestBody](lossClaimSelfEmploymentJson)(path = "/typeOfClaim",
                                                                                replacement = 12344.toJson,
                                                                                expectedError = JsonError.STRING_FORMAT_EXCEPTION)
    }
  }

  "writes" when {
    "passed a valid Loss Claim Employment model" should {
      "return a valid Loss Claim Employment JSON" in {
        Json.toJson(lossClaimSelfEmployment) shouldBe lossClaimSelfEmploymentDownstreamJson
      }
    }
    "passed a valid Loss Claim Property model" should {
      "return a valid Loss Claim Property JSON" in {
        Json.toJson(lossClaimUkPropertyNonFhl) shouldBe lossClaimUkPropertyNonFhlDownstreamJson
      }
    }
    "passed a valid Loss Claim Foreign Property model" should {
      "return a valid Loss Claim Foreign Property JSON" in {
        Json.toJson(lossClaimForeignProperty) shouldBe lossClaimForeignPropertyDowwnstreamJson
      }
    }
  }
}
