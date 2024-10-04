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

package v5.lossClaims.amendOrder.model.request

import play.api.libs.json.Json
import shared.utils.UnitSpec
import v5.lossClaims.amendOrder.def1.model.request.{Claim, Def1_AmendLossClaimsOrderRequestBody}
import v5.lossClaims.common.models.TypeOfClaim

class AmendLossClaimsOrderRequestBodySpec extends UnitSpec {

  private val mtdJson = Json.parse("""{
      |  "typeOfClaim": "carry-sideways",
      |  "listOfLossClaims": [
      |    {
      |      "claimId": "id",
      |      "sequence": 1
      |    }
      |  ]
      |}""".stripMargin)

  private val model = Def1_AmendLossClaimsOrderRequestBody(
    typeOfClaim = TypeOfClaim.`carry-sideways`,
    listOfLossClaims = Seq(Claim(claimId = "id", sequence = 1))
  )

  private val downstreamJson = Json.parse("""{
      |  "claimType": "CSGI",
      |  "claimsSequence": [
      |    {
      |      "claimId": "id",
      |      "sequence": 1
      |    }
      |  ]
      |}""".stripMargin)

  "reads" should {
    "read to a model" in {
      mtdJson.as[Def1_AmendLossClaimsOrderRequestBody] shouldBe model
    }
  }

  "writes" should {
    "read a model to downstream format" in {
      Json.toJson(model) shouldBe downstreamJson
    }
  }

}
