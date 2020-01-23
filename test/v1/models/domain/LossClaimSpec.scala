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

import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v1.models.utils.JsonErrorValidators

class LossClaimSpec extends UnitSpec with JsonErrorValidators {

  val lossClaimEmployment = LossClaim(
    taxYear = "2019-20",
    typeOfLoss = TypeOfLoss.`self-employment`,
    typeOfClaim = TypeOfClaim.`carry-forward`,
    selfEmploymentId = Some("X2IS12356589871")
  )

  val lossClaimEmploymentJson: JsValue = Json.parse(
    """
      |{
      |    "typeOfLoss": "self-employment",
      |    "selfEmploymentId": "X2IS12356589871",
      |    "typeOfClaim": "carry-forward",
      |    "taxYear": "2019-20"
      |}
    """.stripMargin)

  val lossClaimEmploymentDesJson: JsValue = Json.parse(
    """
      |{
      |    "incomeSourceId": "X2IS12356589871",
      |    "reliefClaimed": "CF",
      |    "taxYear": "2020"
      |}
    """.stripMargin)

  val lossClaimProperty = LossClaim(
    taxYear = "2019-20",
    typeOfLoss = TypeOfLoss.`uk-property-non-fhl`,
    typeOfClaim = TypeOfClaim.`carry-forward-to-carry-sideways`,
    selfEmploymentId = None
  )

  val lossClaimPropertyJson: JsValue = Json.parse(
    """
      |{
      |    "typeOfLoss": "uk-property-non-fhl",
      |    "typeOfClaim": "carry-forward-to-carry-sideways",
      |    "taxYear": "2019-20"
      |}
    """.stripMargin)

  val lossClaimPropertyDesJson: JsValue = Json.parse(
    """
      |{
      |    "incomeSourceType": "02",
      |    "reliefClaimed": "CFCSGI",
      |    "taxYear": "2020"
      |}
    """.stripMargin)

  "reads" when {
    "passed a valid LossClaim Json" should {
      "return a valid model" in {
      lossClaimEmploymentJson.as[LossClaim] shouldBe lossClaimEmployment
      }

      testMandatoryProperty[LossClaim](lossClaimEmploymentJson)("/typeOfLoss")
      testPropertyType[LossClaim](lossClaimEmploymentJson)(
        path = "/typeOfLoss",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[LossClaim](lossClaimEmploymentJson)("/taxYear")
      testPropertyType[LossClaim](lossClaimEmploymentJson)(
        path = "/taxYear",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[LossClaim](lossClaimEmploymentJson)("/typeOfClaim")
      testPropertyType[LossClaim](lossClaimEmploymentJson)(
        path = "/typeOfClaim",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)
    }
  }

  "writes" when {
    "passed a valid Loss Claim Employment model" should {
      "return a valid Loss Claim Employment JSON" in {
        LossClaim.writes.writes(lossClaimEmployment) shouldBe lossClaimEmploymentDesJson
      }
    }
    "passed a valid Loss Claim Property model" should {
      "return a valid Loss Claim Property JSON" in {
        LossClaim.writes.writes(lossClaimProperty) shouldBe lossClaimPropertyDesJson
      }
    }
  }


}
