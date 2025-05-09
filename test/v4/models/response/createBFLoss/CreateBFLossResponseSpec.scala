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

package v4.models.response.createBFLoss

import shared.hateoas.{HateoasFactory, HateoasWrapper, Link}
import shared.hateoas.Method.{DELETE, GET, POST}
import shared.config.MockSharedAppConfig
import play.api.libs.json.{JsValue, Json}
import shared.utils.UnitSpec
import v4.models.response.createBFLosses.{CreateBFLossHateoasData, CreateBFLossResponse}

class CreateBFLossResponseSpec extends UnitSpec {

  val lossIdResponse: CreateBFLossResponse = CreateBFLossResponse(lossId = "AAZZ1234567890a")

  val lossIdJson: JsValue = Json.parse(
    """
      |{
      |   "lossId": "AAZZ1234567890a"
      |}
    """.stripMargin
  )

  val lossIdDownstreamJson: JsValue = Json.parse(
    """
      |{
      |   "lossId": "AAZZ1234567890a"
      |}
    """.stripMargin
  )

  "reads" when {
    "passed valid LossIdResponse JSON" should {
      "return a valid model" in {
        lossIdDownstreamJson.as[CreateBFLossResponse] shouldBe lossIdResponse
      }

    }
  }

  "writes" when {
    "given a valid LossIdResponse model" should {
      "return a valid LossIdResponse JSON" in {
        Json.toJson(lossIdResponse) shouldBe lossIdJson
      }
    }
  }

  "HateoasFactory" must {
    class Test extends MockSharedAppConfig {
      val hateoasFactory = new HateoasFactory(mockSharedAppConfig)
      val nino           = "someNino"
      val lossId         = "lossId"
      MockedSharedAppConfig.apiGatewayContext.returns("individuals/losses").anyNumberOfTimes()
    }

    "expose the correct links for create" in new Test {
      hateoasFactory.wrap(lossIdResponse, CreateBFLossHateoasData(nino, lossId)) shouldBe
        HateoasWrapper(
          lossIdResponse,
          Seq(
            Link(s"/individuals/losses/$nino/brought-forward-losses/lossId", GET, "self"),
            Link(s"/individuals/losses/$nino/brought-forward-losses/lossId", DELETE, "delete-brought-forward-loss"),
            Link(s"/individuals/losses/$nino/brought-forward-losses/lossId/change-loss-amount", POST, "amend-brought-forward-loss")
          )
        )
    }
  }

}
