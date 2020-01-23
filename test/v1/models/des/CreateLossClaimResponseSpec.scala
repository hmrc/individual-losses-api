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

package v1.models.des

import mocks.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import v1.models.hateoas.Link
import v1.models.hateoas.Method.{DELETE, GET, POST}

class CreateLossClaimResponseSpec extends UnitSpec with MockAppConfig {

  val createClaimsResponse = CreateLossClaimResponse(id = "AAZZ1234567890a")

  val createClaimsResponseJson: JsValue = Json.parse(
    """
      |{
      |   "id": "AAZZ1234567890a"
      |}
    """.stripMargin)

  val createClaimsResponseDesJson: JsValue = Json.parse(
    """
      |{
      |   "claimId": "AAZZ1234567890a"
      |}
    """.stripMargin)

  "reads" when {
    "passed valid LossIdResponse JSON" should {
      "return a valid model" in {
        createClaimsResponseDesJson.as[CreateLossClaimResponse] shouldBe createClaimsResponse
      }

    }
  }

  "writes" when {
    "passed a valid LossIdResponse model" should {
      "return a valid LossIdResponse JSON" in {
        Json.toJson(createClaimsResponse) shouldBe createClaimsResponseJson
      }
    }
  }

  "The Links Factory" should {

    "return the correct hateoas links" when {

      "provided with a claim id of 'claimId' and nino of 'AA123456A'" in {
        MockedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes

        CreateLossClaimResponse.LinksFactory.links(mockAppConfig, CreateLossClaimHateoasData("AA123456A", "claimId")) shouldBe
        Seq(
          Link("/individuals/losses/AA123456A/loss-claims/claimId", GET, "self"),
          Link("/individuals/losses/AA123456A/loss-claims/claimId", DELETE, "delete-loss-claim"),
          Link("/individuals/losses/AA123456A/loss-claims/claimId/change-type-of-claim", POST, "amend-loss-claim")
        )
      }
    }
  }
}
