/*
 * Copyright 2023 HM Revenue & Customs
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

package v5.lossClaims.create.model.response

import shared.config.MockAppConfig
import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec
import v5.lossClaims.create.def1.model.response.Def1_CreateLossClaimResponse

class CreateLossClaimResponseSpec extends UnitSpec with MockAppConfig {

  val createClaimsResponse: CreateLossClaimResponse = Def1_CreateLossClaimResponse(claimId = "AAZZ1234567890a")

  val createClaimsResponseJson: JsValue = Json.parse(
    """
      |{
      |   "claimId": "AAZZ1234567890a"
      |}
    """.stripMargin
  )

  val createClaimsResponseDownstreamJson: JsValue = Json.parse(
    """
      |{
      |   "claimId": "AAZZ1234567890a"
      |}
    """.stripMargin
  )

  "reads" when {
    "passed valid LossIdResponse JSON" should {
      "return a valid model" in {
        createClaimsResponseDownstreamJson.as[Def1_CreateLossClaimResponse] shouldBe createClaimsResponse
      }

    }
  }

  "writes" when {
    "given a valid LossIdResponse model" should {
      "return a valid LossIdResponse JSON" in {
        Json.toJson(createClaimsResponse) shouldBe createClaimsResponseJson
      }
    }
  }

}
