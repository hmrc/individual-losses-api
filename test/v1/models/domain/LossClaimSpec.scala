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

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.utils.JsonErrorValidators

class LossClaimSpec extends UnitSpec with JsonErrorValidators {

  val lossClaimEmploymentJson = Json.parse(
    """
      |{
      |    "typeOfLoss": "self-employment",
      |    "selfEmploymentId": "X2IS12356589871",
      |    "typeOfClaim": "carry-forward",
      |    "taxYear": "2017-18"
      |}
    """.stripMargin)

  val lossClaimPropertyJson = Json.parse(
    """
      |{
      |    "typeOfLoss": "uk-property-non-fhl",
      |    "typeOfClaim": "carry-forward-to-carry-sideways-general-income",
      |    "taxYear": "2017-18"
      |}
    """.stripMargin)

  val lossClaimEmployment = LossClaim(taxYear = "2017-18",
    typeOfLoss = "self-employment",
    typeOfClaim = "carry-forward",
    selfEmploymentId = Some("X2IS12356589871")
  )

  val lossClaimProperty = LossClaim(taxYear = "2017-18",
    typeOfLoss = "uk-property-non-fhl",
    typeOfClaim = "carry-forward-to-carry-sideways-general-income",
    selfEmploymentId = None
  )

  "reads" when {
    "passed a valid LossClaim Json" should {
      "return a valid model" in {
      lossClaimEmploymentJson.as[LossClaim] shouldBe lossClaimEmployment
      }

/*      testMandatoryProperty[BFLoss](lossClaimEmploymentJson)("/typeOfLoss")
      testPropertyType[BFLoss](lossClaimEmploymentJson)(
        path = "/typeOfLoss",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[BFLoss](lossClaimEmploymentJson)("/taxYear")
      testPropertyType[BFLoss](lossClaimEmploymentJson)(
        path = "/taxYear",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)

      testMandatoryProperty[BFLoss](lossClaimEmploymentJson)("/typeOfClaim")
      testPropertyType[BFLoss](lossClaimEmploymentJson)(
        path = "/typeOfClaim",
        replacement = 12344.toJson,
        expectedError = JsonError.STRING_FORMAT_EXCEPTION)*/
    }
  }



}
